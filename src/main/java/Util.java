import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mysql.cj.protocol.Message;

import java.io.Reader;
import java.sql.*;

interface Validator {
    boolean validate(Object data);

    boolean validate(Object data, String regex);

    String customValidate(Object data);
}

public class Util {
    Connection con;
    JsonObject data;
    Statement stmt;

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

    public void validate(String field, Object data, String regex, Validator validator) throws ValidationException {
        if (validator.validate(data)) throw new ValidationException(field + "should not be empty", 400);
        if (validator.validate(data, regex))
            throw new ValidationException("Invalid format for field '" + field + "'", 400);
        String err_msg = validator.customValidate(data);
        if(!err_msg.isEmpty()) throw new ValidationException(err_msg, 400);
    }
}

class Response {
    boolean success;
    String payload;
    Error error;

    public Response(boolean success, String payload) {
        this.success = success;
        this.payload = payload;
    }

    public Response(boolean success, String payload, Error error) {
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