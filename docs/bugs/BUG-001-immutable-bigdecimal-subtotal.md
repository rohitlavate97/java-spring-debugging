# BUG-001: Unexpected Zero Subtotal Due to Discarded Immutable BigDecimal Result

---

### ROOT CAUSE
In Java, `java.math.BigDecimal` is an immutable class. Methods such as `.add()`, `.subtract()`, `.multiply()`, and `.divide()` do not modify the instance in place; instead, they return a new `BigDecimal` instance containing the result of the arithmetic operation. 

In `OrderPricingService.calculateOrderPricing`, the line:
```java
subtotal.add(itemTotal);
```
invoked `.add()` on the `subtotal` accumulator (`BigDecimal.ZERO`) without reassigning the returned value back to `subtotal`. As a consequence, `subtotal` remained unchanged as `BigDecimal.ZERO` across all loop iterations.

---

### WHY IT HAPPENED
Developers accustomed to mutable accumulator patterns or primitive operators (e.g. `+=`) occasionally assume that calling `.add()` on an object mutates its internal state. Because `BigDecimal` is immutable for precision safety, calling `.add()` without assignment is a silent no-op.

---

### HOW TO PROVE IT
1. **JUnit Test Failure**: An assertion on `summary.getSubtotal()` returned `<0.00>` instead of `<1300.00>`.
2. **Debugger Inspection**:
   - Set a line breakpoint inside the `for (OrderItem item : items)` loop in `OrderPricingService.java`.
   - Step over line `subtotal.add(itemTotal);`.
   - Observe in the **Variables View** that the variable `subtotal` remains `0` after each iteration despite `itemTotal` holding positive values (`1200.00`, `100.00`).
   - Use **Evaluate Expression** (`Alt+F8`) to test `subtotal.add(itemTotal)`: observe that the expression evaluates to `1200.00`, but `subtotal` in scope is unchanged.

---

### INTELLIJ DEBUGGING PROCESS
1. Open [`OrderPricingServiceTest.java`](file:///D:/Projects/Spring%20Boot/java-spring-debugging/src/test/java/com/eopis/order/service/OrderPricingServiceTest.java) and set a breakpoint at line 37 (`pricingService.calculateOrderPricing(...)`).
2. Run test in Debug mode (`Shift+F9`).
3. Press **Step Into (`F7`)** to enter `calculateOrderPricing`.
4. Press **Step Over (`F8`)** through the `for` loop.
5. In the **Variables** pane, watch `subtotal` and `itemTotal`.
6. Notice `subtotal` remains `0` while `itemTotal` is `1200.00`.
7. Select `subtotal.add(itemTotal)` and press **Alt+F8 (Evaluate Expression)** to confirm that `.add()` produces the expected instance, proving the bug is the missing assignment.

---

### FIX
```diff
- subtotal.add(itemTotal);
+ subtotal = subtotal.add(itemTotal);
```

---

### REGRESSION TEST
[`OrderPricingServiceTest.shouldCalculateCorrectPricingForVipCustomerOrder`](file:///D:/Projects/Spring%20Boot/java-spring-debugging/src/test/java/com/eopis/order/service/OrderPricingServiceTest.java) verifies that an order with multiple items properly accumulates subtotal, applies VIP tier discounts, computes tax, and determines shipping thresholds accurately.

---

### WHAT TO REMEMBER
- **All `BigDecimal`, `BigInteger`, `String`, `LocalDate`, `Instant` methods return new instances.** Never call transformation/arithmetic methods on them as standalone statements.
- Use SonarLint or compiler warnings (`@CheckReturnValue`) to catch unused method return values at build time.
