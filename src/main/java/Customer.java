import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/customers")
public class Customer extends HttpServlet {
    static PrintWriter out;
    Util util = new Util();

    JsonElement getInvoices(String id, Util util) throws SQLException, ValidationException {

        String query = id.isEmpty() ? "" : "where i.id = " + id;
        ResultSet rs;

        if (!id.isEmpty()) {
            rs = util.stmt.executeQuery(
                    "select count(id) as count from customer where id = " + id);

            int rowCount = 0;
            if (rs.next()) {
                rowCount = rs.getInt("count");
                System.out.println(rowCount);
            }
            if (rowCount == 0) {
                throw new ValidationException("customer not found", 404);
            }
        }

        String sql = "select name, phone, email, billing_address, shipping_address from customer " + query;

        rs = util.stmt.executeQuery(sql);

        List<CustomerModel> customers = new ArrayList<>();

        while (rs.next()) {
            System.out.println(rs.getLong("id"));
            CustomerModel customer = new CustomerModel(rs.getLong("id"), rs.getString("name"),
                    rs.getLong("phone"), rs.getString("email"), rs.getString("billing_address"),
                    rs.getString("shipping_address"));

            customers.add(customer);
        }

        return new Gson().toJsonTree(customers.size() == 1 ? customers.get(0) : customers);
    }

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        util.initDB();
        util.initGson(req.getReader());
        out = res.getWriter();

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":

                    JsonElement name = util.get("name");
                    JsonElement phone = util.get("phone");
                    JsonElement email = util.get("email");
                    JsonElement billing_address = util.get("billing_address");
                    JsonElement shipping_address = util.get("shipping_address");

                    util.validate("name", name, Util.alphaRegex, true, true, null);
                    util.validate("phone", phone, Util.phoneRegex, true, true, null);
                    util.validate("email", email, Util.emailRegex, true, true, null);
                    util.validate("billing_address", billing_address, Util.alphaRegex, true, true, null);
                    util.validate("shipping_address", billing_address, Util.alphaRegex, true, true, null);

                    int rows = util.stmt.executeUpdate("insert into " +
                            "customer(name, phone, email, billing_address, shipping_address)" +
                            " values(\"" +
                            util.get("name").getAsString() + "\"," +
                            util.get("phone").getAsLong() + ",\"" +
                            util.get("email").getAsString() + "\",\"" +
                            util.get("billing_address").getAsString() + "\",\"" +
                            util.get("shipping_address").getAsString() + "\"" +
                            ")");
                    if (rows != -1) {
                        out.println("Query Executed");
                        util.con.commit();
                    }
                    break;
                case "GET":
                    res.getWriter().print("Get request for taxes");
                    break;
                case "PUT":

                    break;
                case "DELETE":

                    break;
            }
        } catch (Exception e) {
            out.println(e.toString());
        }
    }
}