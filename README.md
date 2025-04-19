# InvoicerApp

### 🇬🇧 English

**InvoicerApp** is a simple web application for creating and managing invoices. Built with **Spring Boot**, **Hibernate**, **Thymeleaf**, and **MySQL**.

### ✅ Features
- Add, edit and delete invoices
- Filter invoices by status (PAID / UNPAID)
- Automatically calculate invoice total
- Store multiple products per invoice (optional future feature)
- User interface in Polish with proper UTF-8 encoding

### 🛠️ Technologies Used
- Java 21
- Spring Boot 3.2+
- Spring Data JPA (Hibernate)
- Thymeleaf
- MySQL

### 🚀 How to Run
1. Create MySQL database, e.g. `invoicer_db`
2. Configure connection in `src/main/resources/application.properties`
<pre>properties
spring.datasource.url=jdbc:mysql://localhost:3306/invoicer_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update</pre>



3. Run the application:

<pre>./mvnw spring-boot:run</pre>

4. Open in browser: [http://localhost:8080/invoices](http://localhost:8080/invoices)



### 🇵🇱 Polski

**InvoicerApp** to prosta aplikacja webowa do tworzenia i zarządzania fakturami. Zbudowana z użyciem **Spring Boot**, **Hibernate**, **Thymeleaf** i **MySQL**.

### ✅ Funkcjonalności
- Dodawanie, edycja i usuwanie faktur
- Filtrowanie faktur po statusie (OPLACONA / NIEOPLACONA)
- Automatyczne wyliczanie sumy faktury
- Obsługa wielu przedmiotów w jednej fakturze (opcjonalnie w przyszłości)
- Interfejs w języku polskim z poprawnym kodowaniem znaków UTF-8

### 🛠️ Technologie
- Java 21
- Spring Boot 3.2+
- Spring Data JPA (Hibernate)
- Thymeleaf
- MySQL

### 🚀 Jak uruchomić
1. Utwórz bazę danych MySQL, np. `invoicer_db`
2. Skonfiguruj połączenie w pliku `src/main/resources/application.properties`:
<pre>properties spring.datasource.url=jdbc:mysql://localhost:3306/invoicer_db
spring.datasource.username=your_username spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update</pre>

3. Uruchom aplikację:

<pre>./mvnw spring-boot:run'</pre>

4. Otwórz w przeglądarce: [http://localhost:8080/invoices](http://localhost:8080/invoices)

