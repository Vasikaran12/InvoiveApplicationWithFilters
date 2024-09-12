import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/test")
public class Test extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Util util = new Util();
        util.initDB();
        PrintWriter out = resp.getWriter();


        try {
            DatabaseMetaData meta = util.con.getMetaData();
            ResultSet columns = meta.getColumns(null,null, "invoice", null);
            while(columns.next())
            {
                String columnName = columns.getString("COLUMN_NAME");
                String datatype = columns.getString("TYPE_NAME");
                String columnSize = columns.getString("COLUMN_SIZE");
                String decimalDigits = columns.getString("DECIMAL_DIGITS");
                String isNullable = columns.getString("IS_NULLABLE");
                String is_autoIncrement = columns.getString("IS_AUTOINCREMENT");
                //Printing results
                System.out.println(columnName + "---" + datatype + "---" + columnSize + "---" + decimalDigits + "---" + isNullable + "---" + is_autoIncrement);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
