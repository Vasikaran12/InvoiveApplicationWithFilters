import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/items")
public class Item extends HttpServlet {
    static PrintWriter out;

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Util util = new Util();
        util.initDB();
        util.initGson(req.getReader());
        out = res.getWriter();

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
                    int rows = util.stmt.executeUpdate("insert into" +
                            " item(name,selling_price,retail_price,stock,tax_percent)" +
                            " values(\"" +
                            util.get("name").getAsString() + "\"," +
                            util.get("selling_price").getAsFloat() + "," +
                            util.get("retail_price").getAsFloat() + "," +
                            util.get("stock").getAsInt() + "," +
                            util.get("tax_percent").getAsLong() +
                            ")");

                    if (rows != -1) {
                        out.println("Query Executed");

                        util.con.commit();

                    }
                    break;
                case "GET":
                    res.getWriter().print("Get request for items");
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