import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/taxes")
public class Tax extends HttpServlet {
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
                            " tax(type, tax)" +
                            " values(" +
                            Util.get("type").getAsInt() + "," +
                            Util.get("tax").getAsInt() +
                            ")");
                    if (rows != -1) {
                        out.println("Query Executed");
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