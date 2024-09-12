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
        Util.initDB();
        Util.initGson(req.getReader());
        out = res.getWriter();

        String method = req.getMethod();

        try {
            switch (method) {
                case "POST":
                    int rows = Util.stmt.executeUpdate("insert into" +
                            " item(name,selling_price,retail_price,stock,tax_percent)" +
                            " values(\"" +
                            Util.get("name").getAsString() + "\"," +
                            Util.get("selling_price").getAsFloat() + "," +
                            Util.get("retail_price").getAsFloat() + "," +
                            Util.get("stock").getAsInt() + "," +
                            Util.get("tax_percent").getAsLong() +
                            ")");

                    if (rows != -1) {
                        out.println("Query Executed");

                        Util.con.commit();

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