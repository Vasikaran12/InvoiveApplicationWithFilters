public class CustomerModel {
    private long id;
    private String name;
    private long phone;
    private String email;
    private String billing_address;
    private String shipping_address;

    public CustomerModel(long id, String name, long phone, String email, String billing_address, String shipping_address) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.billing_address = billing_address;
        this.shipping_address = shipping_address;
    }
}