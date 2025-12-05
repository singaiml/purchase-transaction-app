# Purchase Transaction Application - API Testing Guide

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

```bash
curl -s http://localhost:8080/api/v1/transactions/550e8400-e29b-41d4-a716-446655440000 | jq .
```

### 4. Delete Transaction
**DELETE** `/api/v1/transactions/{transactionId}`

```bash
curl -X DELETE http://localhost:8080/api/v1/transactions/550e8400-e29b-41d4-a716-446655440000 | jq .
```

**Example Response:**
```json
{
  "message": "Transaction deleted successfully",
  "transactionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 5. Get Available Currencies
**GET** `/api/v1/conversions/currencies/available`

```bash
curl -s http://localhost:8080/api/v1/conversions/currencies/available | jq .
```

**Example Response:**
```json
[
  "EUR",
  "GBP",
  "JPY",
  "CAD",
  "AUD",
  "CHF",
  "CNY",
  "SEK",
  "NZD",
  "MXN",
  "SGD",
  "HKD",
  "NOK",
  "KRW",
  "TRY",
  "RUB",
  "INR",
  "BRL",
  "ZAR"
]
```

### 6. Convert Transaction to Foreign Currency
**GET** `/api/v1/conversions/{transactionId}?currency={currencyCode}`

```bash
curl -s "http://localhost:8080/api/v1/conversions/550e8400-e29b-41d4-a716-446655440000?currency=EUR" | jq .
```

**Example Response:**
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

Conversion rules (business logic):

- When converting between currencies, the service will use the latest available exchange rate whose effective date is less than or equal to the purchase date, but not older than 6 months before the purchase date.
- If no exchange rate is available within that 6-month window (on or before the purchase date), the service returns HTTP 503 with a message stating the purchase cannot be converted to the target currency.
- Converted amounts are rounded to two decimal places (cents) using half-up rounding.

Example error response when no suitable rate is found:

```json
{
  "error": "Service Unavailable",
  "message": "Cannot convert purchase to target currency EUR: no exchange rate within 6 months on or before 2025-12-01",
  "timestamp": "2025-12-04T23:55:24.266398",
  "status": 503
}
```
```

## Complete Test Workflow

### Step 1: Create Multiple Transactions
```bash
TXID1=$(curl -s -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Conference%20Pass&transactionDate=2025-11-30&amount=599.99" | jq -r '.transactionId')

TXID2=$(curl -s -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Team%20Lunch&transactionDate=2025-12-03&amount=125.75" | jq -r '.transactionId')

TXID3=$(curl -s -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Software%20License&transactionDate=2025-12-02&amount=299.00" | jq -r '.transactionId')

echo "Created transactions: $TXID1, $TXID2, $TXID3"
```

### Step 2: View All Transactions
```bash
curl -s http://localhost:8080/api/v1/transactions | jq '.[] | {id: .transactionId, desc: .description, amount: .amount}'
```

### Step 3: Get Currency List
```bash
curl -s http://localhost:8080/api/v1/conversions/currencies/available | jq '.[]'
```

### Step 4: Convert Transaction to Multiple Currencies
```bash
# Convert to EUR
curl -s "http://localhost:8080/api/v1/conversions/$TXID1?currency=EUR" | jq '{original: .originalAmountUsd, currency: .currencyCode, converted: .convertedAmount}'

# Convert to GBP
curl -s "http://localhost:8080/api/v1/conversions/$TXID1?currency=GBP" | jq '{original: .originalAmountUsd, currency: .currencyCode, converted: .convertedAmount}'

# Convert to JPY
curl -s "http://localhost:8080/api/v1/conversions/$TXID1?currency=JPY" | jq '{original: .originalAmountUsd, currency: .currencyCode, converted: .convertedAmount}'
```

### Step 5: Delete a Transaction
```bash
curl -X DELETE "http://localhost:8080/api/v1/transactions/$TXID2" | jq .
```

### Step 6: Verify Deletion
```bash
curl -s http://localhost:8080/api/v1/transactions | jq 'length'
```

## Error Cases to Test

### Invalid Description (Empty)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=&transactionDate=2025-12-01&amount=100"
```

### Invalid Description (Too Long)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=$(python3 -c 'print(\"a\"*51)')&transactionDate=2025-12-01&amount=100"
```

### Invalid Amount (Zero)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Test&transactionDate=2025-12-01&amount=0"
```

### Invalid Amount (Negative)
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Test&transactionDate=2025-12-01&amount=-50"
```

### Future Date
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -d "description=Test&transactionDate=2025-12-10&amount=100"
```

### Transaction Not Found
```bash
curl -s http://localhost:8080/api/v1/transactions/non-existent-id
```

### Invalid Currency
```bash
curl -s "http://localhost:8080/api/v1/conversions/550e8400-e29b-41d4-a716-446655440000?currency=XXX"
```

## Testing with Postman

Import these requests:

**Create Transaction:**
- Method: POST
- URL: http://localhost:8080/api/v1/transactions
- Params: description, transactionDate, amount

**Get All Transactions:**
- Method: GET
- URL: http://localhost:8080/api/v1/transactions

**Get Single Transaction:**
- Method: GET
- URL: http://localhost:8080/api/v1/transactions/{{transactionId}}

**Delete Transaction:**
- Method: DELETE
- URL: http://localhost:8080/api/v1/transactions/{{transactionId}}

**Get Available Currencies:**
- Method: GET
- URL: http://localhost:8080/api/v1/conversions/currencies/available

**Convert Transaction:**
- Method: GET
- URL: http://localhost:8080/api/v1/conversions/{{transactionId}}?currency=EUR

## Data Storage

Transactions are persisted in: `./data/transactions.json`

To reset data:
```bash
rm ./data/transactions.json
```

## Logging

View application logs in real-time:
- DEBUG level for com.purchase.transaction
- INFO level for root

Check logs at startup or review application output.
