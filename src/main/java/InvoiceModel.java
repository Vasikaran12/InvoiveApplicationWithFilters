import java.util.*;


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

public class InvoiceModel {
    private long id;
    private int customer_id;
    private String date;
    private String due_date;
    private float sub_total;
    private int shipping_charge;
    private int adjustments;
    private int discount_type;
    private int discount;
    private float total;
    private String status;
    private float amountPaid;
    private float remaining_amount;
    private List<InvoiceItemModel> items;
    private List<PaymentModel> payments;

    static Map<String, String> dbModel = new HashMap<String, String>() {
        {
            put("customer_id", "customer_id");
            put("date", "date");
            put("due_date", "due_date");
            put("sub_total", "sub_total");
            put("shipping_charge", "shipping_charge");
            put("adjustments", "adjustments");
            put("discount_type", "discount_type");
            put("discount", "discount");
            put("total", "total");
        }
    };

    public InvoiceModel(long id, int customer_id, String date, String due_date, float sub_total, int shipping_charge,
                        int adjustments, int discount_type, int discount, float total, List<InvoiceItemModel> items) {
        this.id = id;
        this.customer_id = customer_id;
        this.date = date;
        this.due_date = due_date;
        this.sub_total = sub_total;
        this.shipping_charge = shipping_charge;
        this.adjustments = adjustments;
        this.discount_type = discount_type;
        this.discount = discount;
        this.total = total;
        this.items = items;
    }

    InvoiceModel(long id, String date, String due_date, int shipping_charge, float sub_total, float total, int customer_id,
                 String status, float amount_paid, float remaining_amount) {
        this.id = id;
        this.date = date;
        this.due_date = due_date;
        this.shipping_charge = shipping_charge;
        this.sub_total = sub_total;
        this.total = total;
        this.customer_id = customer_id;
        this.status = status;
        this.amountPaid = amount_paid;
        this.remaining_amount = remaining_amount;
    }

    void setItems(List<InvoiceItemModel> items) {
        this.items = items;
    }

    void setPayments(List<PaymentModel> payments) {
        this.payments = payments;
    }
}