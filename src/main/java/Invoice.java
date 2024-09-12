import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.xml.internal.ws.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class InvoiceItemModel {
    int id;
    String item_name;
    float selling_price;
    int quantity;
    float discount_percent;
    float sub_total;
    float total;

    public InvoiceItemModel(int id, String item_name, float selling_price, float sub_total, int quantity, float discount_percent, float total) {
        this.id = id;
        this.item_name = item_name;
        this.selling_price = selling_price;
        this.quantity = quantity;
        this.discount_percent = discount_percent;
        this.sub_total = sub_total;
        this.total = total;
    }
}


@WebServlet("/invoices/*")
public class Invoice extends HttpServlet {
    PrintWriter out;

    String getInvoices(String id, Util util) throws SQLException, ValidationException {

        String query = id.isEmpty() ? ";" : " where i.id = " + id;
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

        rs = util.stmt.executeQuery(
                "select i.id, i.date, i.due_date, i.shipping_charge, i.sub_total, i.total, i.customer_id from invoice i" + query);

        List<InvoiceModel> invoices = new ArrayList<>();

        while (rs.next()) {
            InvoiceModel invoice = new InvoiceModel(rs.getLong("id"), rs.getString("date"),
                    rs.getString("due_date"), rs.getInt("shipping_charge"), rs.getFloat("sub_total"),
                    rs.getFloat("total"), rs.getInt("customer_id"));

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
            invoice.setItems(InvoiceItems);
            invoices.add(invoice);
        }

        return new Gson().toJson(invoices.size() == 1 ? invoices.get(0) : invoices);
    }

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Util util = new Util();
        util.initDB();
        util.initGson(req.getReader());
        out = res.getWriter();
        res.setContentType("application/json");

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

                    Map<String, JsonElement> json = util.data.getAsJsonObject().asMap();
                    Map<String, String> db = InvoiceModel.dbModel;
//
//                    ResultSet columns = util.getColumns("invoice");
//                    while(columns.next())
//                    {
//                        String columnName = columns.getString("COLUMN_NAME");
//                        String datatype = columns.getString("TYPE_NAME");
//                        boolean isNullable = columns.getString("IS_NULLABLE").equals("YES");
//
//                        if(!isNullable && !json.containsKey(columnName)){
//
//                        }else{
//
//                        }
//                    }

                    JsonElement date = util.get("date");
                    JsonElement due_date = util.get("due_date");
                    JsonElement customer_id = util.get("customer_id");
                    JsonElement shipping_charge = util.get("shipping_charge");
                    JsonElement tax_type = util.get("tax_type");
                    JsonElement tax_percent = util.get("tax_percent");
                    JsonElement discount_type = util.get("discount_type");
                    JsonElement discount = util.get("discount");
                    JsonElement adjustments = util.get("adjustments");

                    //validation
                    if (date == null) {
                        throw new ValidationException("date should not be empty", 403);
                    }
                    if (due_date == null){
                        throw new ValidationException("due_date should not be empty", 403);
                    }
                    if (customer_id == null) {
                        throw new ValidationException("customer_id should not be empty", 403);
                    }
                    if (shipping_charge == null) {
                        throw new ValidationException("Shipping charge should not be empty", 403);
                    }
                    if (tax_type == null) {
                        throw new ValidationException("tax_type should not be empty", 403);
                    }
                    if (tax_percent == null) {
                        throw new ValidationException("tax_percent should not be empty", 403);
                    }



                    if (date.getAsString().isEmpty()){
                        throw new ValidationException("Date should not be empty", 403);
                    }
                    if (due_date.getAsString().isEmpty()) {
                        throw new ValidationException("Due Date should not be empty", 403);
                    }
                    if (format.parse(date.getAsString()).after(format.parse(due_date.getAsString()))) {
                        throw new ValidationException("Due date should be after the Invoice date", 403);
                    }
                    if (shipping_charge.getAsInt() < 0) {
                        throw new ValidationException("Invalid shipping charge", 403);
                    }
                    if (tax_type.getAsInt() != 0 && tax_type.getAsInt() != 1) {
                        throw new ValidationException("Invalid tax type", 403);
                    }
                    if (tax_percent.getAsInt() <= 0 || tax_percent.getAsInt() > 100) {
                        throw new ValidationException("Invalid tax percent", 403);
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

                    if (discount_type != null && (discount_type.getAsInt() != 0 && discount_type.getAsInt() != 1)) {
                        throw new ValidationException("Invalid discount_type", 403);
                    }
                    if (discount_type != null && (discount_type.getAsInt() == 0 && (discount.getAsInt() <= 0 || discount.getAsInt() > 100))) {
                        throw new ValidationException("Invalid discount percent", 403);
                    }

                    PreparedStatement ps = util.con.prepareStatement(
                            "insert into" + " invoice" +
                                    "(date, due_date, shipping_charge, sub_total, total, customer_id, tax_percent," +
                                    "tax_amount, discount_type, discount, adjustments, tax_type, discount_amount)" +
                                    " values(?,?,?,0,0,?,?,0,?,?,?,?,0);", Statement.RETURN_GENERATED_KEYS);
//                                    dbFormat.format(format.parse(date.getAsString())) + "','" +
//                                    dbFormat.format(format.parse(due_date.getAsString())) + "'," +
//                                    shipping_charge + "," +
//                                    customer_id + "," +
//                                    tax_type + "," +
//                                    tax_percent + "," +
//                                    0 + "," +
//                                    0 + ")",);

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

                    JsonElement items = util.get("items");

                    if (items == null) {
                        throw new ValidationException("Items should not be empty", 403);
                    }

                    rs = ps.getGeneratedKeys();
                    rs.next();
                    long invoice_id = rs.getLong(1);

                    float sub_total = 0;

                    for (JsonElement e : items.getAsJsonArray()) {
                        JsonObject ob = e.getAsJsonObject();

                        long item_id = ob.get("id").getAsLong();

                        rs = util.stmt.executeQuery("select count(id) as count from item where id = " + item_id);

                        rowCount = 0;
                        if (rs.next()) {
                            rowCount = rs.getInt("count");
                            System.out.println(rowCount);
                        }

                        if (rowCount == 0) {
                            throw new ValidationException("Item (item_id: " + item_id + ") not found", 404);
                        }

                        int quantity = ob.get("quantity").getAsInt();
                        float discount_percent = ob.get("discount_percent").getAsInt();

                        rs = util.stmt.executeQuery(
                                "select name, selling_price, stock, tax_percent from item where id = " + item_id);
                        rs.next();
                        String name = rs.getString("name");
                        float selling_price = rs.getFloat("selling_price");
                        int stock = rs.getInt("stock") - quantity;

                        if (stock < 0) {
                            throw new ValidationException("Insufficient Stock for item id " + item_id, 403);
                        }

                        float item_tax_percent = rs.getInt("tax_percent");
                        float item_sub_total = selling_price * quantity - (discount_percent / 100) * selling_price;
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
                        throw new ValidationException("Invalid discount percent", 403);
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

                    util.con.commit();
                    out.println(getInvoices(invoice_id + "", util));
                    res.setStatus(201);

                    break;

                case "GET":
                    String id = req.getPathInfo();

                    if (id != null) {
                        id = id.replace("/", "");
                    } else {
                        id = "";
                    }

                    String response = getInvoices(id, util);

                    out.println(response);
                    break;
                case "PUT":

                case "DELETE":

                    break;
            }
        } catch (ValidationException e) {
            res.setStatus(403);
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