
# Purchase Transaction Application - API Testing Guide

```markdown

This is the canonical testing and API reference for the project. It includes quick-start instructions, endpoint reference, full API test workflows, error-case examples, Postman import notes, config, and troubleshooting.

## Technology stack

- Spring Boot 3.2.0
- Java 17 LTS
- Maven 3.9+
- JUnit 5, Mockito, AssertJ
- Jackson for JSON
- RestTemplate for HTTP

## Build and run:

```bash
cd purchase-transaction-app
mvn clean package -DskipTests
java -jar target/purchase-transaction-app-1.0.0.jar
```

The application will be available at `http://localhost:8080`.


## Application Running
The application is running on `http://localhost:8080`

## API Endpoints

### 1. Create a New Transaction
**POST** `/api/v1/transactions`

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
	-H "Content-Type: application/x-www-form-urlencoded" \
	-d "description=Laptop%20Purchase&transactionDate=2025-12-01&amount=1250.50"
```

**Example Response:**
```json
{
	"transactionId": "550e8400-e29b-41d4-a716-446655440000",
	"description": "Laptop Purchase",
	"transactionDate": "2025-12-01",
	"amount": 1250.50,
	"createdAt": "2025-12-04"
}
```

### 2. Get All Transactions
**GET** `/api/v1/transactions`

```bash
curl -s http://localhost:8080/api/v1/transactions | jq .
```

**Example Response:**
```json
[
	{
		"transactionId": "550e8400-e29b-41d4-a716-446655440000",
		"description": "Laptop Purchase",
		"transactionDate": "2025-12-01",
		"amount": 1250.50,
		"createdAt": "2025-12-04"
	},
	{
		"transactionId": "660e8400-e29b-41d4-a716-446655440001",
		"description": "Office Supplies",
		"transactionDate": "2025-12-02",
		"amount": 75.25,
		"createdAt": "2025-12-04"
	}
]
```

### 3. Get Specific Transaction
**GET** `/api/v1/transactions/{transactionId}`

## API Endpoints (reference)

- POST `/api/v1/transactions` — Create new transaction (form fields: `description`, `transactionDate`, `amount`)
- GET `/api/v1/transactions` — Get all transactions
- GET `/api/v1/transactions/{id}` — Get transaction by ID
- DELETE `/api/v1/transactions/{id}` — Delete transaction
- GET `/api/v1/conversions/currencies/available` — Get available currencies
- GET `/api/v1/conversions/{id}?currency={code}` — Convert transaction to currency

### Examples

Create a transaction:

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "description=Laptop%20Purchase&transactionDate=2025-12-01&amount=1250.50"
```

Get all transactions:

```bash
curl -s http://localhost:8080/api/v1/transactions | jq .
```

Convert a transaction:

```bash
curl -s "http://localhost:8080/api/v1/conversions/550e8400-e29b-41d4-a716-446655440000?currency=EUR" | jq .
```

Conversion example response:

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Laptop Purchase",
  "transactionDate": "2025-12-01",
  "originalAmountUsd": 1250.50,
  "currencyCode": "EUR",
  "exchangeRate": 0.92,
  "convertedAmount": 1150.46,
  "exchangeRateDate": "2025-12-04"
}
```

Conversion rules (business logic):

- Use the latest available exchange rate with effective date <= purchase date and not older than 6 months before the purchase date.
- If no rate is available within that 6-month window (on or before the purchase date), the service returns HTTP 503 with a descriptive message.
- Converted amounts are rounded to two decimals using HALF_UP.

Example error when no rate found:

```json
{
  "error": "Service Unavailable",
  "message": "Cannot convert purchase to target currency EUR: no exchange rate within 6 months on or before 2025-12-01",
  "timestamp": "2025-12-04T23:55:24.266398",
  "status": 503
}
```

## Complete Test Workflow (API)

The following scripts exercise end-to-end API behavior and are handy for manual regression testing.

### Create test transactions

```bash
TXID1=$(curl -s -X POST http://localhost:8080/api/v1/transactions \
	-H "Content-Type: application/x-www-form-urlencoded" \
	-d "description=Conference%20Pass&transactionDate=2025-11-30&amount=599.99" | jq -r '.transactionId')

TXID2=$(curl -s -X POST http://localhost:8080/api/v1/transactions \
	-H "Content-Type: application/x-www-form-urlencoded" \
	-d "description=Team%20Lunch&transactionDate=2025-12-03&amount=125.75" | jq -r '.transactionId')

TXID3=$(curl -s -X POST http://localhost:8080/api/v1/transactions \
	-H "Content-Type: application/x-www-form-urlencoded" \
	-d "description=Software%20License&transactionDate=2025-12-02&amount=299.00" | jq -r '.transactionId')

echo "Created transactions: $TXID1, $TXID2, $TXID3"
```

### Convert and verify

```bash
curl -s "http://localhost:8080/api/v1/conversions/$TXID1?currency=EUR" | jq '{original: .originalAmountUsd, currency: .currencyCode, converted: .convertedAmount}'
```

### Cleanup

```bash
curl -X DELETE "http://localhost:8080/api/v1/transactions/$TXID2" | jq .
```

## Error cases to exercise (API)

- Empty/too-long description (>=51 chars)
- Amount = 0 or negative
- Future transaction date
- Invalid currency code
- Transaction not found

Example invalid amount:

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Test&transactionDate=2025-12-01&amount=0"
```

## Running tests (unit & integration)

Run full test suite:

```bash
mvn clean test
```

Run a single test class:

```bash
mvn -Dtest=PurchaseTransactionServiceTest test
```

Run with coverage (JaCoCo):

```bash
mvn test jacoco:report
```

Notes:

- Integration tests are written using `MockRestServiceServer` to stub Treasury API responses so tests do not rely on live HTTP calls.
- Some tests use Spring (`@SpringBootTest`) and others are pure unit tests with Mockito.

## Postman

You can import the cURL examples above into Postman. Typical requests to import:

- Create Transaction (POST)
- Get All Transactions (GET)
- Get Transaction by ID (GET)
- Delete Transaction (DELETE)
- Get Available Currencies (GET)
- Convert Transaction (GET)

## Configuration

Edit `src/main/resources/application.properties` or override on the command line:

```properties
server.port=8080
logging.level.root=INFO
logging.level.com.purchase.transaction=DEBUG
app.repository.path=./data
app.exchange-rate.cache-enabled=true
```

## Data storage

Transactions are persisted in `./data/transactions.json`.
Remove this file to reset data:

```bash
rm ./data/transactions.json
```

## Troubleshooting

- Port 8080 in use:

```bash
java -jar target/purchase-transaction-app-1.0.0.jar --server.port=8081
```

- Clean build:

```bash
mvn clean
```

- Increase logging:

```bash
java -jar target/purchase-transaction-app-1.0.0.jar --logging.level.root=DEBUG
```
## Notes

- `API_Testing_Guide.md` is now the canonical testing and API manual. 

`````