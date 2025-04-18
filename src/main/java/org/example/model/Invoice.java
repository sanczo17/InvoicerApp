package org.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Invoice { @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
    @NotBlank(message = "Nazwa klienta jest wymagana")
    private String customerName;

    @NotBlank(message = "Nazwa produktu jest wymagana")
    private String product;

    @Min(value = 1, message = "Ilość musi być większa od 0")
    private int quantity;

    @Min(value = 0, message = "Cena nie może być ujemna")
    private double price;

    @NotNull(message = "Status jest wymagany")
    private InvoiceStatus status;

        public Invoice() {
        }

        public Invoice(Long id, String customerName, String product, int quantity, double price, InvoiceStatus status) {
            this.id = id;
            this.customerName = customerName;
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public InvoiceStatus getStatus() {
            return status;
        }

        public void setStatus(InvoiceStatus status) {
            this.status = status;
        }

        public double getTotal() {
            return quantity * price;
        }

    }