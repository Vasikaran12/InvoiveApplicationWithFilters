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

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Util.initDB();
        Util.initGson(req.getReader());
        out = res.getWriter();

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
                    int rows = Util.stmt.executeUpdate("insert into " +
                            "customer(name, phone, email, billing_address, shipping_address)" +
                            " values(\"" +
                            Util.get("name").getAsString() + "\"," +
                            Util.get("phone").getAsLong() + ",\"" +
                            Util.get("email").getAsString() + "\",\"" +
                            Util.get("billing_address").getAsString() + "\",\"" +
                            Util.get("shipping_address").getAsString() + "\"" +
                            ")");
                    if (rows != -1) {
                        out.println("Query Executed");

                        Util.con.commit();

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