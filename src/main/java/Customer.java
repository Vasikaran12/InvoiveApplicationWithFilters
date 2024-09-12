import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



@WebServlet("/customers")
public class Customer extends HttpServlet {
    static PrintWriter out;
    Util util = new Util();

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        util.initDB();
        util.initGson(req.getReader());
        out = res.getWriter();

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
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