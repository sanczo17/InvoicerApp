# InvoicerApp

### 🇬🇧 English

**InvoicerApp** is a web application for creating and managing invoices. Built with **Spring Boot**, **Hibernate**, **Thymeleaf**, and **MySQL**.

### ✅ Features
- User authentication and role-based access control
- Add, edit and delete invoices
- Filter invoices by status (PAID / UNPAID)
- Search invoices by date range, customer, amount
- Automatically calculate invoice totals
- Customer management
- PDF invoice generation
- Responsive user interface with Polish localization

### 🛠️ Technologies Used
- Java 21
- Spring Boot 3.2+
- Spring Security
- Spring Data JPA (Hibernate)
- Thymeleaf
- MySQL
- iText PDF

### 🚀 How to Run
1. Create MySQL database, e.g. `invoicer_db`
2. Configure connection in `src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/invoicer_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

4. Open in browser: [http://localhost:8080](http://localhost:8080)

### 👤 Default Credentials
Upon first run, the system creates a default administrator account:
- Username: `admin`
- Password: `admin`

You will be prompted to change this password on first login.

---

### 🇵🇱 Polski

**InvoicerApp** to aplikacja webowa do tworzenia i zarządzania fakturami. Zbudowana z użyciem **Spring Boot**, **Hibernate**, **Thymeleaf** i **MySQL**.

### ✅ Funkcjonalności
- Uwierzytelnianie użytkowników i kontrola dostępu oparta na rolach
- Dodawanie, edycja i usuwanie faktur
- Filtrowanie faktur po statusie (OPŁACONA / NIEOPŁACONA)
- Wyszukiwanie faktur według zakresu dat, klienta, kwoty
- Automatyczne wyliczanie sum faktur
- Zarządzanie klientami
- Generowanie faktur PDF
- Responsywny interfejs użytkownika w języku polskim

### 🛠️ Technologie
- Java 21
- Spring Boot 3.2+
- Spring Security
- Spring Data JPA (Hibernate)
- Thymeleaf
- MySQL
- iText PDF

### 🚀 Jak uruchomić
1. Utwórz bazę danych MySQL, np. `invoicer_db`
2. Skonfiguruj połączenie w pliku `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/invoicer_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

3. Uruchom aplikację:
```bash
./mvnw spring-boot:run
```

4. Otwórz w przeglądarce: [http://localhost:8080](http://localhost:8080)

### 👤 Domyślne dane logowania
Przy pierwszym uruchomieniu system tworzy domyślne konto administratora:
- Nazwa użytkownika: `admin`
- Hasło: `admin`

Przy pierwszym logowaniu zostaniesz poproszony o zmianę hasła.