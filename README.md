
## 2. External Service Resilience

### a) Dalam kondisi apa retry tidak boleh dilakukan?

**Tidak boleh retry:**
- HTTP 4xx (400, 401, 403, 404, 409) - client error, retry sia-sia
- Non-idempotent operations (POST yang sudah sukses partial)
- Business logic errors (validation, business rules)
- Timeout > 30 detik - indikasi masalah serius

**Boleh retry:**
- HTTP 5xx (server errors)
- Network errors (timeout, connection refused)
- HTTP 429 (rate limit) dengan backoff
- Idempotent operations (GET, PUT, DELETE)

### b) Apa perbedaan retry dan circuit breaker?

| Retry | Circuit Breaker |
|-------|-----------------|
| Coba ulang tiap request gagal | Putus koneksi jika terlalu banyak failure |
| Per-request | Per-service |
| Stateless | Stateful (Closed → Open → Half-Open) |
| Bisa lambat | Fail fast |

**Analogi:** Retry = coba terus ketuk pintu. Circuit Breaker = stop ketuk pintu, tunggu 30 detik, coba lagi.

### c) Apa risiko retry tanpa jitter atau backoff?

**Risiko:**
1. **Thundering Herd** - semua client retry bersamaan, server makin down
2. **Resource Exhaustion** - thread pool habis, memory leak
3. **Cascade Failure** - service A down → service B down → domino effect

## 3. Distributed Logging & Tracing

### a) Informasi minimum apa yang wajib ada di setiap log?

```json
{
  "timestamp": "2025-01-26T10:30:45Z",
  "traceId": "abc-123",           // track journey
  "spanId": "def-456",            // track operation
  "serviceName": "order-service",
  "level": "ERROR",
  "message": "Order failed",
  "userId": "user-123",
  "method": "POST",
  "path": "/api/orders",
  "statusCode": 500,
  "duration": 1234
}
```

### b) Bagaimana cara menelusuri satu request dari awal sampai akhir?

**Gunakan Distributed Tracing:**

1. **Generate traceId** di API Gateway
2. **Propagate traceId** ke semua service via HTTP header
3. **Log dengan traceId** di setiap service
4. **Query logs** dengan traceId yang sama

### c) Apa dampak jika traceId tidak konsisten antar service?

**Dampak:**
- Tidak bisa trace request end-to-end
- Debug jadi nightmare
- "Data hilang" tidak bisa ditelusuri
- Root cause analysis impossible
- Mean Time To Resolution (MTTR) meningkat drastis

---

## 4. Message Broker & Event Ordering

### a) Bagaimana memastikan consumer bersifat idempotent?

**Strategi:**

1. **Unique Event ID + Database Check**
```java
@Transactional
public void handleEvent(CustomerUpdatedEvent event) {
    if (processedEvents.exists(event.getEventId())) {
        return; // sudah diproses, skip
    }
    
    processCustomerUpdate(event);
    processedEvents.save(event.getEventId());
}
```

2. **Natural Idempotency Key**
```java
// Gunakan field unik sebagai idempotency key
UPDATE customers 
SET name = 'New Name', version = version + 1
WHERE customer_id = '123' AND version = 5;
// Jika version tidak match, update gagal
```

3. **Upsert Pattern**
```java
// Last write wins
customerRepo.save(customer); // update or insert
```

### b) Apakah ordering dijamin oleh broker atau consumer?

**Kafka:** Ordering dijamin **per partition**
```
Topic: customer-events
├─ Partition 0: customer-1 events (ordered)
├─ Partition 1: customer-2 events (ordered)
└─ Partition 2: customer-3 events (ordered)
```
- Gunakan `customerId` sebagai partition key
- Ordering antar partition TIDAK dijamin

**RabbitMQ:** Ordering dijamin **per queue**
- Satu consumer per queue
- Multiple consumer = ordering tidak dijamin

**Consumer harus handle ordering jika:**
- Multiple consumers
- Retry mechanism
- Processing async

### c) Apa dampaknya jika ordering tidak terjaga?

**Dampak:**

1. **Data Inconsistency**
```
Event 1: Update email to "new@email.com" (timestamp: 10:00)
Event 2: Update email to "old@email.com" (timestamp: 09:00)

Jika Event 2 diproses duluan → email jadi "new@email.com" ✓
Jika Event 1 diproses duluan → email jadi "old@email.com" ✗
```

2. **Business Logic Error**
```
Event 1: Order Created
Event 2: Order Paid
Event 3: Order Shipped

Jika Event 3 duluan → ship unpaid order ✗
```

3. **State Corruption** - entity dalam state invalid

**Solusi:**
- Gunakan version/timestamp untuk detect stale events
- Design idempotent operations
- Event sourcing untuk rebuild state

---

## 5. Caching Strategy

### a) Kapan cache harus di-invalidate?

**Invalidate saat:**
1. **Data berubah** (write-through pattern)
```java
public void updateProduct(Product product) {
    productRepo.save(product);
    cache.evict("product:" + product.getId()); // invalidate
}
```

2. **TTL expire** (Time-To-Live)
```java
cache.put("product:123", product, Duration.ofMinutes(5));
```

3. **Cache full** (LRU/LFU eviction)

**Strategi:**
- **Write-Through**: update DB + cache bersamaan
- **Write-Behind**: update cache dulu, DB async
- **Cache-Aside**: invalidate cache, biarkan lazy load

### b) Apakah cache boleh dijadikan source of truth?

**TIDAK!**

**Alasan:**
- Cache bisa hilang (restart, eviction, expire)
- Cache corruption tidak recoverable
- Data di cache tidak durable
- Database adalah source of truth

**Pengecualian:**
- Session data (sudah bersifat temporary)
- Real-time leaderboard
- Rate limiting counter

**Golden Rule:** Cache = optimization, bukan storage

### c) Bagaimana mencegah cache stampede?

**Cache Stampede:** ribuan request hit database bersamaan saat cache expire.

**Solusi:**

1. **Lock-Based Refresh**
```java
public Product getProduct(String id) {
    Product cached = cache.get(id);
    if (cached != null) return cached;
    
    synchronized(id.intern()) { // lock per key
        cached = cache.get(id); // double check
        if (cached != null) return cached;
        
        Product product = db.findById(id);
        cache.put(id, product);
        return product;
    }
}
```

2. **Probabilistic Early Refresh**
```java
// Refresh cache sebelum expire
double expiryTime = 300; // 5 menit
double delta = 60; // refresh 1 menit sebelumnya
double randomFactor = Math.random() * delta;

if (timeLeft < randomFactor) {
    refreshCache(); // async
}
```

3. **Stale-While-Revalidate**
```java
// Serve stale data sambil refresh background
cache.getStaleWhileRevalidate(key, 
    () -> db.findById(key) // refresh function
);
```

---

## 6. Microservice Security

### a) Apakah penggunaan API key sudah cukup?

**TIDAK CUKUP untuk production!**

**Kelemahan API Key:**
- Static, susah rotate
- Tidak ada expiry
- Tidak ada user/service identity
- Bocor = revoke semua service
- Tidak ada fine-grained permission

**Yang lebih baik:**
- mTLS (mutual TLS)
- OAuth2 + JWT
- Service Mesh (Istio, Linkerd)

### b) Bagaimana mencegah service palsu mengakses API internal?

**Strategi:**

1. **mTLS (Mutual TLS)**
```
Client cert → Server verify cert → Allow/Deny
```
- Setiap service punya certificate
- Service palsu tidak punya valid cert

2. **Service Mesh**
```
Istio/Linkerd: Zero-trust network
- Encrypt all traffic
- Verify service identity
- Policy enforcement
```

3. **API Gateway + Internal Network**
```
Internet → API Gateway → Internal Service (private network)
```
- Internal service tidak exposed ke internet
- Firewall rules

4. **JWT with Service Claims**
```json
{
  "iss": "auth-service",
  "sub": "order-service",
  "aud": "payment-service",
  "permissions": ["payment.process"]
}
```

### c) Apa risiko penggunaan JWT tanpa expiry pendek untuk komunikasi internal?

**Risiko:**

1. **Token Theft/Leakage**
```
JWT leaked → attacker gunakan sampai expire
Long expiry = long window of attack
```

2. **Credential Rotation Delayed**
```
Service compromised → revoke token
Tapi token masih valid 30 hari → attacker masih bisa akses
```

3. **Permission Changes Delayed**
```
Revoke permission dari service A
Tapi JWT masih claim permission lama
```

4. **Compliance Issues**
- PCI-DSS, SOC2 require short-lived tokens
- Internal JWT: 5-15 menit expiry
- Gunakan refresh token jika perlu
- Atau lebih baik: mTLS (no token, verify cert)
