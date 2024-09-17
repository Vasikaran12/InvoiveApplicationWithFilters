import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/invoices/*")
public class Invoice extends HttpServlet {
    PrintWriter out;

    JsonElement getInvoices(String id, Util util) throws SQLException, ValidationException {

        String query = id.isEmpty() ? "where " : "where i.id = " + id + " and ";
        ResultSet rs;

        if (!id.isEmpty()) {
            rs = util.stmt.executeQuery(
                    "select count(id) as count from invoice where id = " + id);

            int rowCount = 0;
            if (rs.next()) {
                rowCount = rs.getInt("count");
                System.out.println(rowCount);
            }
            if (rowCount == 0) {
                throw new ValidationException("Invoice not found", 404);
            }
        }

        String sql = "select i.id, i.date, i.due_date, i.shipping_charge, i.sub_total, i.total, i.customer_id, sum(ip.amount) as amount_paid, ins.status as status, ip.invoice_id" +
                " from invoice i left outer join invoice_status ins on i.id = ins.invoice_id" +
                " left outer join invoice_payment ip on i.id = ip.invoice_id " +
                query + "ins.id = (select id from invoice_status where invoice_id = i.id order by id desc limit 1) " +
                "group by ip.invoice_id";

        rs = util.stmt.executeQuery(sql);

        List<InvoiceModel> invoices = new ArrayList<>();

        while (rs.next()) {
            System.out.println(rs.getLong("id"));
            InvoiceModel invoice = new InvoiceModel(rs.getLong("id"), rs.getString("date"),
                    rs.getString("due_date"), rs.getInt("shipping_charge"), rs.getFloat("sub_total"),
                    rs.getFloat("total"), rs.getInt("customer_id"), rs.getString("status"),
                    rs.getFloat("amount_paid"), rs.getFloat("total") - rs.getFloat("amount_paid"));

            id = rs.getLong("id") + "";

            ResultSet itemsRs = util.con.createStatement().executeQuery(
                    "select id, item_name, quantity ,selling_price, discount_percent , sub_total, total, invoice_id from invoice_item where invoice_id = " + id);

            List<InvoiceItemModel> InvoiceItems = new ArrayList<>();

            while (itemsRs.next()) {
                InvoiceItemModel InvoiceItem = new InvoiceItemModel(itemsRs.getInt("id"),
                        itemsRs.getString("item_name"), itemsRs.getFloat("selling_price")
                        , itemsRs.getInt("sub_total"), itemsRs.getInt("quantity"),
                        itemsRs.getInt("discount_percent"),
                        itemsRs.getFloat("total"));
                InvoiceItems.add(InvoiceItem);
            }

            ResultSet paymentsRs = util.con.createStatement().executeQuery(
                    "select p.id, p.transaction_number, p.mode, p.paid_at, ip.amount from payment p inner join invoice_payment ip on p.id = ip.payment_id where ip.invoice_id = " + id);

            List<PaymentModel> payments = new ArrayList<>();

            while (paymentsRs.next()) {
                PaymentModel payment = new PaymentModel(paymentsRs.getInt("id"),
                        paymentsRs.getInt("mode"), paymentsRs.getInt("transaction_number")
                        , paymentsRs.getFloat("amount"), paymentsRs.getString("paid_at"));
                payments.add(payment);
            }

            invoice.setItems(InvoiceItems);
            invoice.setPayments(payments);
            invoices.add(invoice);
        }

        return new Gson().toJsonTree(invoices.size() == 1 ? invoices.get(0) : invoices);
    }

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Util util = new Util();
        util.initDB();
        util.data = (JsonObject) req.getAttribute("reqJson");
        out = res.getWriter();
        res.setContentType("application/json");

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

                    JsonElement date = util.get("date");
                    JsonElement due_date = util.get("due_date");
                    JsonElement customer_id = util.get("customer_id");
                    JsonElement shipping_charge = util.get("shipping_charge");
                    JsonElement tax_type = util.get("tax_type");
                    JsonElement tax_percent = util.get("tax_percent");
                    JsonElement discount_type = util.get("discount_type");
                    JsonElement discount = util.get("discount");
                    JsonElement adjustments = util.get("adjustments");
                    JsonElement items = util.get("items");

                    //validation
                    util.validate("date", date, Util.dateRegex, true, true, null);

                    util.validate("due_date", due_date, Util.dateRegex, true, true, null);

                    util.validate("customer_id", customer_id, Util.numericRegex, true, true, null);

                    util.validate("shipping_charge", shipping_charge, Util.numericRegex, true, true, null);

                    util.validate("tax_type", tax_type, "^[01]$", true, true, null);

                    util.validate("tax_percent", tax_percent, Util.percentRegex, true, true, null);

                    util.validate("items", items, "", true, false, null);

                    if (format.parse(date.getAsString()).after(format.parse(due_date.getAsString()))) {
                        throw new ValidationException("Due date should be after the Invoice date", 403);
                    }

                    ResultSet rs = util.stmt.executeQuery(
                            "select count(id) as count from customer where id = " + customer_id);

                    int rowCount = 0;
                    if (rs.next()) {
                        rowCount = rs.getInt("count");
                        System.out.println(rowCount);
                    }
                    if (rowCount == 0) {
                        throw new ValidationException("Customer not found", 404);
                    }

                    util.validate("discount_type", discount_type, "^[01]$", false, true, null);

                    util.validate("adjustments", adjustments, Util.floatRegex, false, true, null);

                    if (discount_type != null) {
                        util.validate("discount", discount,
                                discount_type.getAsInt() == 0 ? Util.percentRegex : Util.numericRegex,
                                true, true, null);
                    }

                    PreparedStatement ps = util.con.prepareStatement(
                            "insert into" + " invoice" +
                                    "(date, due_date, shipping_charge, sub_total, total, customer_id, tax_percent," +
                                    "tax_amount, discount_type, discount, adjustments, tax_type, discount_amount)" +
                                    " values(?,?,?,0,0,?,?,0,?,?,?,?,0);", Statement.RETURN_GENERATED_KEYS);

                    ps.setDate(1, Date.valueOf(date.getAsString()));
                    ps.setDate(2, Date.valueOf(due_date.getAsString()));
                    ps.setInt(3, shipping_charge.getAsInt());
                    ps.setInt(4, customer_id.getAsInt());
                    ps.setInt(5, tax_percent.getAsInt());
                    ps.setInt(6, discount_type != null ? discount_type.getAsInt() : 0);
                    ps.setFloat(7, discount_type != null ? discount.getAsFloat() : 0);
                    ps.setInt(8, adjustments != null ? adjustments.getAsInt() : 0);
                    ps.setInt(9, tax_type.getAsInt());

                    ps.executeUpdate();

                    rs = ps.getGeneratedKeys();
                    rs.next();
                    long invoice_id = rs.getLong(1);

                    float sub_total = 0;

                    for (JsonElement e : items.getAsJsonArray()) {
                        JsonObject ob = e.getAsJsonObject();

                        JsonElement id = ob.get("id");
                        JsonElement quantity = ob.get("quantity");
                        JsonElement discount_percent = ob.get("discount_percent");

                        util.validate("items (id: " + id + "): id", id, Util.numericRegex, true, true, null);

                        util.validate("items (id: " + id + "): quantity", quantity, Util.numericRegex, true, true,
                                null);

                        util.validate("items: (id: " + id + "): discount_percent", discount_percent, Util.numericRegex,
                                true, true, null);

                        long item_id = ob.get("id").getAsLong();

                        rs = util.stmt.executeQuery("select count(id) as count from item where id = " + item_id);

                        rowCount = 0;
                        if (rs.next()) {
                            rowCount = rs.getInt("count");
                            System.out.println(rowCount);
                        }

                        if (rowCount == 0) {
                            throw new ValidationException("items (id: " + item_id + ") not found", 404);
                        }

                        rs = util.stmt.executeQuery(
                                "select name, selling_price, stock, tax_percent from item where id = " + item_id);
                        rs.next();
                        String name = rs.getString("name");
                        float selling_price = rs.getFloat("selling_price");
                        int stock = rs.getInt("stock") - quantity.getAsInt();

                        if (stock < 0) {
                            throw new ValidationException("Insufficient Stock for item (id: " + item_id + ")", 403);
                        }

                        float item_tax_percent = rs.getInt("tax_percent");
                        float item_sub_total = selling_price * quantity.getAsInt() - (discount_percent.getAsFloat() / 100) * selling_price;
                        float item_tax_amount = (item_tax_percent / 100) * item_sub_total;
                        float item_total = item_sub_total + item_tax_amount;

                        sub_total += item_total;

                        rs = util.stmt.executeQuery(
                                "select name, selling_price, stock from item where id = " + item_id);

                        util.stmt.addBatch(
                                "insert into invoice_item(" + "item_name, quantity, selling_price, discount_percent, tax_percent, tax_amount, sub_total, total, invoice_id" + ") " + "values(\"" + name + "\"," + quantity + "," + selling_price + "," + discount_percent + "," + item_tax_percent + "," + item_tax_amount + "," + item_sub_total + ", " + item_total + "," + invoice_id + ");");

                        util.stmt.addBatch(
                                "update item " + "set stock = " + stock + " where id = " + item_id);
                    }

                    //tax_type = 0 -- TCS && tax_type = 1 -- TDS
                    float tax_amount = (tax_percent.getAsFloat() / 100) * sub_total;

                    if (discount_type != null && (discount_type.getAsInt() == 1 && discount.getAsInt() > sub_total)) {
                        throw new ValidationException("discount should not exceed the sub_total", 400);
                    }

                    float invoice_discount = 0;
                    if (discount_type != null) {
                        if (discount_type.getAsInt() == 0) {
                            invoice_discount = sub_total * (discount.getAsFloat() / 100);
                        } else {
                            invoice_discount = discount.getAsFloat();
                        }
                    }

                    int adjustments_amount = adjustments != null ? adjustments.getAsInt() : 0;

                    if (adjustments_amount < 0 && Math.abs(adjustments_amount) > sub_total) {
                        throw new ValidationException("Invalid adjustments", 403);
                    }

                    float temp = shipping_charge.getAsInt() + sub_total + (adjustments_amount) - invoice_discount;
                    float total = tax_type.getAsInt() == 0 ? temp + tax_amount : temp - tax_amount;

                    util.stmt.addBatch(
                            "update invoice set sub_total = " + sub_total +
                                    ", total = " + total +
                                    ", tax_amount = " + tax_amount +
                                    ", discount_amount = " + invoice_discount +
                                    ", adjustments = " + adjustments_amount +
                                    " where id = " + invoice_id);

                    util.stmt.addBatch(
                            "insert into invoice_status(invoice_id, status, status_changed_at) " +
                                    "values( " +
                                    invoice_id + "," +
                                    "'pending'" + "," +
                                    "CURRENT_TIMESTAMP)");

                    util.stmt.executeBatch();

                    util.sendResponse(out, new Response(true, getInvoices(invoice_id + "", util)), res);
                    util.con.commit();
                    res.setStatus(201);
                    break;

                case "GET":
                    String id = req.getPathInfo();

                    if (id != null) {
                        id = id.replace("/", "");
                    } else {
                        id = "";
                    }
                    util.sendResponse(out, new Response(true, getInvoices(id, util)), res);

                    break;
                case "PUT":
                    JsonElement put_id = util.get("id");
                    JsonElement status = util.get("status");

                    util.validate("id", put_id, Util.numericRegex, true, true, null);

                    rs = util.stmt.executeQuery(
                            "select count(id) as count from invoice where id = " + put_id);

                    rowCount = 0;
                    if (rs.next()) {
                        rowCount = rs.getInt("count");
                        System.out.println(rowCount);
                    }
                    if (rowCount == 0) {
                        throw new ValidationException("Invoice not found", 404);
                    }

                    rs = util.stmt.executeQuery(
                            "select status from invoice_status where invoice_id = " + put_id + " order by id desc limit 1");

                    if (rs.next()) {
                        String sts = rs.getString("status");
                        if (sts.equals("confirmed")) {
                            throw new ValidationException("Invoice (id: " + put_id + ") is already confirmed", 400);
                        } else if (sts.contains("paid")) {
                            throw new ValidationException("Invoice (id: " + put_id + ") is already paid", 400);
                        }
                    }

                    util.validate("status", status, "\\bconfirmed\\b", true, true, null);

                    util.stmt.executeUpdate("insert into invoice_status(invoice_id, status, status_changed_at) " +
                            "values( " +
                            put_id + "," +
                            "'confirmed'" + "," +
                            "CURRENT_TIMESTAMP)");

                    util.sendResponse(out, new Response(true, getInvoices(put_id + "", util)), res);
                    util.con.commit();
                    res.setStatus(200);

                case "DELETE":

                    break;
            }
        } catch (ValidationException e) {
            res.setStatus(e.errorCode);
            out.println(new Gson().toJson(
                    new Response(false, new Error(e.errorCode, e.getMessage()))));
        } catch (Exception e) {
            res.sendError(500, e.getMessage());
            try {
                util.con.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}