import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/payments/*") public class Payment extends HttpServlet {
    PrintWriter out;

    void updatePayments(String invoice_id, float amount, Util util) throws SQLException {

    }

    JsonElement getPayments(String id, Util util) throws SQLException, ValidationException {

        String query = id.isEmpty() ? ";" : " where id = " + id;
        ResultSet rs;

        if (!id.isEmpty()) {
            rs = util.stmt.executeQuery("select count(id) as count from payment where id = " + id);

            int rowCount = 0;
            if (rs.next()) {
                rowCount = rs.getInt("count");
                System.out.println(rowCount);
            }
            if (rowCount == 0) {
                throw new ValidationException("Payment not found", 404);
            }
        }

        rs = util.stmt.executeQuery(
                "select p.id, p.mode, p.transaction_number, p.amount, p.paid_at from payment p" + query);

        List<PaymentModel> payments = new ArrayList<>();

        while (rs.next()) {
            PaymentModel payment = new PaymentModel(rs.getLong("id"), rs.getInt("mode"),
                    rs.getInt("transaction_number"), rs.getFloat("amount"), rs.getString("paid_at"));

            id = rs.getLong("id") + "";

            ResultSet itemsRs = util.con.createStatement().executeQuery(
                    "select i.id, i.total, ins.status , ip.amount from invoice_payment ip inner join invoice i on i.id = ip.invoice_id inner join invoice_status ins on i.id = ins.invoice_id where ip.payment_id = " + id + " and ins.id = (select id from invoice_status where invoice_id = i.id order by id desc limit 1)");

            List<InvoicePaymentModel> invoices = new ArrayList<>();

            while (itemsRs.next()) {
                InvoicePaymentModel invoice = new InvoicePaymentModel(itemsRs.getInt("i.id"),
                        itemsRs.getFloat("i.total"), itemsRs.getFloat("amount"), itemsRs.getString("status"));
                invoices.add(invoice);
            }

            payment.setInvoices(invoices);
            payments.add(payment);
        }

        return new Gson().toJsonTree(payments.size() == 1 ? payments.get(0) : payments);
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

                    JsonElement mode = util.get("mode");
                    JsonElement transactionNumber = util.get("transaction_number");
                    JsonElement invoices = util.get("invoices");


                    //validation
                    util.validate("mode", mode, "[0-3]", true, true, null);

                    util.validate("transaction_number", transactionNumber, Util.numericRegex, true, true, null);

                    util.validate("invoices", invoices, "", true, false, null);

                    PreparedStatement ps = util.con.prepareStatement(
                            "insert into" + " payment" + "(mode, transaction_number, amount, paid_at)" + " values(?,?,0, CURRENT_TIMESTAMP);",
                            Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, mode.getAsInt());
                    ps.setInt(2, transactionNumber.getAsInt());

                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    long payment_id = rs.getLong(1);

                    float total_amount = 0;

                    for (JsonElement e : invoices.getAsJsonArray()) {
                        JsonObject ob = e.getAsJsonObject();

                        JsonElement id = ob.get("id");
                        JsonElement amount = ob.get("amount");

                        util.validate("items (id: " + id + "): id", id, Util.numericRegex, true, true, null);
                        util.validate("amount", amount, Util.floatRegex, true, true, null);

                        long invoice_id = id.getAsLong();

                        rs = util.stmt.executeQuery(
                                "select count(id) as count from invoice where id = " + invoice_id);

                        int rowCount = 0;
                        if (rs.next()) {
                            rowCount = rs.getInt("count");
                            System.out.println(rowCount);
                        }
                        if (rowCount == 0) {
                            throw new ValidationException("Invoice (id: " + invoice_id + ") not found", 404);
                        }

                        rs = util.stmt.executeQuery(
                                "select status from invoice_status where invoice_id = " + invoice_id + " order by id desc limit 1");


                        float total = 0;
                        if (rs.next()) {
                            String status = rs.getString("status");
                            if (status.equals("pending")) {
                                throw new ValidationException("Invoice (id: " + invoice_id + ") is still pending", 400);
                            } else if (status.equals("paid")) {
                                throw new ValidationException("Invoice (id: " + invoice_id + ") is already paid", 400);
                            } else if (status.equals("partially paid")) {
                                rs = util.stmt.executeQuery(
                                        "select i.total-sum(ip.amount) as total from invoice i inner join invoice_payment ip on i.id = ip.invoice_id group by ip.invoice_id having ip.invoice_id = " + invoice_id);
                                rs.next();

                                total = rs.getFloat("total");
                            } else {
                                rs = util.stmt.executeQuery(
                                        "select i.total from invoice i where i.id = " + invoice_id);
                                rs.next();

                                total = rs.getFloat("total");
                            }
                        }

                        if (total < amount.getAsFloat()) {
                            throw new ValidationException(
                                    "Payment amount exceeds invoice (id: " + invoice_id + ") total by Rs. " + (amount.getAsFloat() - total),
                                    400);
                        } else if (total > amount.getAsFloat()) {
                            util.stmt.addBatch("insert into invoice_status(invoice_id, status, status_changed_at) " +
                                    "values( " +
                                    invoice_id + "," +
                                    "'partially paid'" + "," +
                                    "CURRENT_TIMESTAMP)");
                        } else {
                            util.stmt.addBatch("insert into invoice_status(invoice_id, status, status_changed_at) " +
                                    "values( " +
                                    invoice_id + "," +
                                    "'paid'" + "," +
                                    "CURRENT_TIMESTAMP)");
                        }

                        float update_amount = Math.min(total, amount.getAsFloat());
                        util.stmt.addBatch("insert into invoice_payment(invoice_id, payment_id, amount) values(" +
                                invoice_id + "," +
                                payment_id + "," +
                                update_amount +
                                ")");

                        total_amount += amount.getAsFloat();
                    }

                    util.stmt.addBatch("update payment set amount = " + total_amount + " where id = " + payment_id);

                    util.stmt.executeBatch();

                    out.println(getPayments(payment_id + "", util));
                    util.con.commit();
                    break;
                case "GET":
                    String id = req.getPathInfo();

                    if (id != null) {
                        id = id.replace("/", "");
                    } else {
                        id = "";
                    }

                    util.sendResponse(out, new Response(true, getPayments(id, util)), res);
                    break;
                case "PUT":

                case "DELETE":

                    break;
            }
        } catch (ValidationException e) {
            res.setStatus(e.errorCode);
            util.sendResponse(out, new Response(false, new Error(e.errorCode, e.getMessage())), res);
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