import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mysql.cj.protocol.Message;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.*;

interface Validator {
    ValidationException validate(Object data);
}

public class Util {
    Connection con;
    JsonObject data;
    Statement stmt;

    static String dateRegex = "^((2000|2400|2800|(19|2[0-9])(0[48]|[2468][048]|[13579][26]))-02-29)$" +
            "|^(((19|2[0-9])[0-9]{2})-02-(0[1-9]|1[0-9]|2[0-8]))$" +
            "|^(((19|2[0-9])[0-9]{2})-(0[13578]|10|12)-(0[1-9]|[12][0-9]|3[01]))$" +
            "|^(((19|2[0-9])[0-9]{2})-(0[469]|11)-(0[1-9]|[12][0-9]|30))$";
    static String alphaRegex = "^[A-Za-z]+$";
    static String numericRegex = "^[0-9]+$";
    static String alphaNumericRegex = "^[A-Za-z0-9]+$";
    static String percentRegex = "^([0-9]|[1-9][0-9]|100)$";
    static String floatRegex = "[-+]?[0-9]*\\.?[0-9]+";
    static String phoneRegex = "^[\\d]{10}$";
    static String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    public void initGson(Reader json) {
        this.data = new Gson().fromJson(json, JsonObject.class);
    }

    public JsonElement get(String key) {
        return this.data.get(key);
    }

    public ResultSet getColumns(String table) throws SQLException {
        DatabaseMetaData meta = this.con.getMetaData();
        return meta.getColumns(null, null, table, null);
    }

    public void initDB() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/InvoiceApp", "root", "");
            this.stmt = this.con.createStatement();

            con.setAutoCommit(false);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void validate(String field, Object data, String regex, boolean nullCheck, boolean regexCheck, Validator validator) throws
            ValidationException {
        if (nullCheck && (data == null || data.toString().replace("\"", "").isEmpty())) {
            throw new ValidationException("'" + field + "' is required", 400);
        }

        if (data != null) {
            data = data.toString().replace("\"", "");
            if (regexCheck && !(data.toString().matches(regex))) {
                System.out.println(data.toString());
                throw new ValidationException("Invalid value for field '" + field + "'", 400);
            }
            if (validator != null) {
                ValidationException exp = validator.validate(data);
                if (exp != null) throw exp;
            }
        }
    }

    public void sendResponse(PrintWriter out, Response response, HttpServletResponse res){
        out.println(new Gson().toJson(response));
        if(response.error != null){
            res.setStatus(response.error.code);
        }
    }
}

class Response {
    boolean success;
    Object payload;
    Error error = null;

    public Response(boolean success, Object payload) {
        this.success = success;
        this.payload = payload;
    }

    public Response(boolean success, Object payload, Error error) {
        this.success = success;
        this.payload = payload;
        this.error = error;
    }

    public Response(boolean success, Error error) {
        this.error = error;
        this.success = success;
    }
}

class Error {
    int code;
    String message;

    public Error(int code, String message) {
        this.code = code;
        this.message = message;
    }
}



class ValidationException extends Exception {
    int errorCode;

    public ValidationException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}