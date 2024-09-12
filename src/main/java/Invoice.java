import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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

class InvoiceModel {
    private long id;
    private String date;
    private String due_date;
    private int shipping_charge;
    private float sub_total;
    private float total;
    private int customer_id;
    private List<InvoiceItemModel> items;

    InvoiceModel(long id, String date, String due_date, int shipping_charge, float sub_total, float total, int customer_id) {
        this.id = id;
        this.date = date;
        this.due_date = due_date;
        this.shipping_charge = shipping_charge;
        this.sub_total = sub_total;
        this.total = total;
        this.customer_id = customer_id;
    }

    void setItems(List<InvoiceItemModel> items) {
        this.items = items;
    }
}

@WebServlet("/invoices/*") public class Invoice extends HttpServlet {
    static PrintWriter out;

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Util.initDB();
        Util.initGson(req.getReader());
        out = res.getWriter();
        res.setContentType("application/json");

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
                    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");

                    String date = Util.get("date").getAsString();
                    String due_date = Util.get("due_date").getAsString();
                    int shipping_charge = Util.get("shipping_charge").getAsInt();
                    long customer_id = Util.get("customer_id").getAsLong();
                    int tax_type = Util.get("tax_type").getAsInt();
                    float tax_percent = Util.get("tax_percent").getAsInt();

                    Util.stmt.executeUpdate(
                            "insert into" + " invoice(date, due_date, shipping_charge, customer_id, tax_type, tax_percent, sub_total, total)" +
                                    " values('" +
                                    dbFormat.format(format.parse(date)) + "','" +
                                    dbFormat.format(format.parse(due_date)) + "'," +
                                    shipping_charge + "," +
                                    customer_id + "," +
                                    tax_type + "," +
                                    tax_percent + "," +
                                    0 + "," +
                                    0 + ")",
                            Statement.RETURN_GENERATED_KEYS);
                    int rows;

                    JsonArray items = Util.get("items").getAsJsonArray();
                    ResultSet rs;

                    rs = Util.stmt.getGeneratedKeys();
                    rs.next();
                    Long invoice_id = rs.getLong(1);

                    float sub_total = 0;

                    for (JsonElement e : items) {
                        JsonObject ob = e.getAsJsonObject();

                        Long item_id = ob.get("id").getAsLong();
                        int quantity = ob.get("quantity").getAsInt();
                        float discount_percent = ob.get("discount_percent").getAsInt();

                        rs = Util.stmt.executeQuery(
                                "select name, selling_price, stock, tax_percent from item where id = " + item_id);
                        rs.next();
                        String name = rs.getString("name");
                        float selling_price = rs.getFloat("selling_price");
                        int stock = rs.getInt("stock") - quantity;
                        float item_tax_percent = rs.getInt("tax_percent");
                        float item_sub_total = selling_price * quantity - (discount_percent / 100) * selling_price;
                        float item_tax_amount = (item_tax_percent / 100) * item_sub_total;
                        float item_total = item_sub_total + item_tax_amount;

                        sub_total += item_total;

                        rs = Util.stmt.executeQuery(
                                "select name, selling_price, stock from item where id = " + item_id);

                        Util.stmt.addBatch(
                                "insert into invoice_item(" + "item_name, quantity, selling_price, discount_percent, tax_percent, tax_amount, sub_total, total, invoice_id" + ") " + "values(\"" + name + "\"," + quantity + "," + selling_price + "," + discount_percent + "," + item_tax_percent + "," + item_tax_amount + "," + item_sub_total + ", " + item_total + "," + invoice_id + ");");

                        Util.stmt.addBatch(
                                "update item " + "set stock = " + stock + " where id = " + item_id);
                    }

                    //tax_type = 0 -- TCS && tax_type = 1 -- TDS
                    float tax_amount = (tax_percent / 100) * sub_total;
                    float total = tax_type == 0 ? shipping_charge + sub_total + tax_amount : shipping_charge + sub_total - tax_amount;

                    Util.stmt.addBatch(
                            "update invoice set sub_total = " + sub_total + ", total = " + total + ", tax_amount = " + tax_amount + " where id = " + invoice_id);

                    Util.stmt.executeBatch();

                    Util.con.commit();
                    res.setStatus(201);
                    break;

                case "GET":
                    String id = req.getPathInfo();


                    if (id != null) {
                        id = id.replace("/", "");
                    } else {
                        id = "";
                    }

                    String query = id.isEmpty() ? ";" : " where i.id = " + id;

                    rs = Util.stmt.executeQuery(
                            "select i.id, i.date, i.due_date, i.shipping_charge, i.sub_total, i.total, i.customer_id from invoice i" + query);

                    while (rs.next()) {
                        InvoiceModel invoice = new InvoiceModel(rs.getLong("id"), rs.getString("date"),
                                rs.getString("due_date"), rs.getInt("shipping_charge"), rs.getFloat("sub_total"),
                                rs.getFloat("total"), rs.getInt("customer_id"));

                        id = rs.getLong("id") + "";


                        ResultSet itemsRs = Util.con.createStatement().executeQuery(
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
                    }

                    break;
                case "PUT":

                    break;
                case "DELETE":

                    break;
            }
        } catch (Exception e) {
            out.println(e.toString());
            try {
                Util.con.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}