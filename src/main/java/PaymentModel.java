import java.util.List;

class InvoicePaymentModel {
    int invoice_id;
    float total;
    float amount_paid;
    String status;

    public InvoicePaymentModel(int invoice_id, float total, float amount_paid, String status) {
        this.invoice_id = invoice_id;
        this.total = total;
        this.amount_paid = amount_paid;
        this.status = status;
    }
}

public class PaymentModel {
    private long id;
    private int mode;
    private int transaction_number;
    private float amount;
    private String paid_at;
    private List<InvoicePaymentModel> invoices;

    public PaymentModel(long id, int mode, int transaction_number, float amount, String paid_at) {
        this.id = id;
        this.mode = mode;
        this.transaction_number = transaction_number;
        this.amount = amount;
        this.paid_at = paid_at;
    }

    void setInvoices(List<InvoicePaymentModel> invoices){
        this.invoices = invoices;
    }
}