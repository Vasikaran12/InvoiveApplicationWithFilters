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

                    //validation
                    if(date.isEmpty()){
                        throw new ValidationException("Date should not be empty");
                    }

                    if(due_date.isEmpty()){
                        throw new ValidationException("Due Date should not be empty");
                    }

                    if (format.parse(date).after(format.parse(due_date))) {
                        throw new ValidationException("Due date should be after the Invoice date");
                    }

                    if(shipping_charge < 0){
                        throw new ValidationException("Invalid shipping charge");
                    }

                    ResultSet rs = Util.stmt.executeQuery("select count(id) as count from customer where id = " + customer_id);

                    int rowCount =  0;
                    if(rs.next()) {
                        rowCount = rs.getInt("count");
                        System.out.println(rowCount);
                    }

                    if(rowCount == 0){
                        throw new ValidationException("Customer not found");
                   }

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

                    JsonArray items = Util.get("items").getAsJsonArray();

                    rs = Util.stmt.getGeneratedKeys();
                    rs.next();
                    long invoice_id = rs.getLong(1);

                    float sub_total = 0;

                    for (JsonElement e : items) {
                        JsonObject ob = e.getAsJsonObject();

                        long item_id = ob.get("id").getAsLong();

                        rs = Util.stmt.executeQuery("select count(id) as count from item where id = " + item_id);

                        rowCount =  0;
                        if(rs.next()) {
                            rowCount = rs.getInt("count");
                            System.out.println(rowCount);
                        }

                        if(rowCount == 0){
                            throw new ValidationException("Item (item_id: "+ item_id + ") not found");
                        }

                        int quantity = ob.get("quantity").getAsInt();
                        float discount_percent = ob.get("discount_percent").getAsInt();

                        rs = Util.stmt.executeQuery(
                                "select name, selling_price, stock, tax_percent from item where id = " + item_id);
                        rs.next();
                        String name = rs.getString("name");
                        float selling_price = rs.getFloat("selling_price");
                        int stock = rs.getInt("stock") - quantity;

                        if(stock < 0){
                            throw new ValidationException("Insufficient Stock for item id " + item_id);
                        }

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
        } catch (ValidationException e) {
            res.setStatus(403);
            out.println(new Gson().toJson(
                    new Response(false, new Error(403, e.getMessage()))));
        } catch (Exception e) {
            res.sendError(500, e.getMessage());
            try {
                Util.con.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}