# BUG-001: Unexpected Zero Subtotal Due to Discarded Immutable BigDecimal Result

[![Bug ID: BUG-001](https://img.shields.io/badge/Bug%20ID-BUG--001-blue.svg)](#incident-metadata)
[![Difficulty: Level 1](https://img.shields.io/badge/Difficulty-Level%201%20(Fundamentals)-green.svg)](#incident-metadata)
[![Module: Order](https://img.shields.io/badge/Module-Order%20Pricing-orange.svg)](#incident-metadata)
[![Status: Solved](https://img.shields.io/badge/Status-Solved%20%26%20Verified-brightgreen.svg)](#incident-metadata)

> **Navigation**: [Home](../../README.md) / [Architecture](../ARCHITECTURE.md) / [System State](../STATE.md) / **BUG-001 Analysis** / [Scored Report](../debugging-reports/REPORT-001-bigdecimal-subtotal.md)

---

## Table of Contents

- [Incident Metadata](#incident-metadata)
- [Executive Summary](#executive-summary)
- [Symptom & Reproduction](#symptom--reproduction)
- [Root Cause Analysis](#root-cause-analysis)
  - [The Immutability Contract](#the-immutability-contract)
  - [Faulty Implementation](#faulty-implementation)
- [Why It Happened](#why-it-happened)
- [Proof & Verification](#proof--verification)
  - [Automated Test Failure](#automated-test-failure)
  - [Debugger State Inspection](#debugger-state-inspection)
- [IntelliJ IDEA Step-by-Step Debugging Walkthrough](#intellij-idea-step-by-step-debugging-walkthrough)
- [Remediation & Code Fix](#remediation--code-fix)
- [Regression Testing](#regression-testing)
- [Architectural Takeaways & Defensive Engineering](#architectural-takeaways--defensive-engineering)
- [Related Resources](#related-resources)

---

## Incident Metadata

| Attribute | Value |
| :--- | :--- |
| **Incident ID** | `BUG-001` |
| **Target Component** | [`OrderPricingService.java`](../../src/main/java/com/eopis/order/service/OrderPricingService.java) |
| **Method** | `calculateOrderPricing(Customer, List<OrderItem>, String)` |
| **Difficulty Level** | **Level 1 — Fundamentals** |
| **Failure Classification** | Logic Defect / Value Discard / Immutability Misconception |
| **Impact** | All order subtotals and resultant order totals calculated as `0.00` |
| **Automated Test** | [`OrderPricingServiceTest.java`](../../src/test/java/com/eopis/order/service/OrderPricingServiceTest.java) |
| **Evaluation Report** | [`REPORT-001-bigdecimal-subtotal.md`](../debugging-reports/REPORT-001-bigdecimal-subtotal.md) |
| **Status** | **RESOLVED & VERIFIED** |

---

## Executive Summary

During checkout processing within the Order module, orders containing valid items with positive pricing were failing final checkout validation or generating zero-dollar totals. 

Unit test verification in [`OrderPricingServiceTest`](../../src/test/java/com/eopis/order/service/OrderPricingServiceTest.java) failed with an assertion mismatch: expected `<1300.00>`, but actual subtotal was `<0.00>`. 

Investigation using IntelliJ IDEA's interactive debugger isolated the defect to line 41 of `OrderPricingService.java`: arithmetic operations on `java.math.BigDecimal` return newly instantiated values rather than mutating in-place. The caller discarded the return value, leaving the running subtotal accumulator unmodified at `BigDecimal.ZERO`.

---

## Symptom & Reproduction

When an order containing multiple items (e.g., Item 1 at \$1,200.00 and Item 2 at \$100.00) is submitted to `OrderPricingService.calculateOrderPricing(...)`, the computed `OrderPricingSummary` contains:

```text
Expected Subtotal : 1300.00
Actual Subtotal   : 0.00
Resultant Tax     : 0.00
Resultant Total   : 0.00
```

### Reproduction Steps

1. Execute the targeted Maven unit test:
   ```bash
   ./mvnw test -Dtest=OrderPricingServiceTest
   ```
2. Observe the assertion failure:
   ```text
   org.opentest4j.AssertionFailedError: 
   expected: <1300.00>
    but was: <0.00>
       at com.eopis.order.service.OrderPricingServiceTest.shouldCalculateCorrectPricingForVipCustomerOrder(OrderPricingServiceTest.java:48)
   ```

---

## Root Cause Analysis

### The Immutability Contract

In Java, arithmetic wrapper objects such as `java.math.BigDecimal` and `java.math.BigInteger` are **strictly immutable**. Their internal representations cannot change after instantiation. 

Methods such as `.add()`, `.subtract()`, `.multiply()`, and `.divide()` compute the arithmetic result and instantiate a **new** `BigDecimal` holding the result.

```text
Initial State:
  subtotal ───────────────► [ BigDecimal: 0.00 ]

After subtotal.add(itemTotal):
  subtotal ───────────────► [ BigDecimal: 0.00 ]   <-- (Still points to original instance!)
                            [ BigDecimal: 1200.00 ] <-- (Orphaned result discarded by GC)
```

### Faulty Implementation

In [`OrderPricingService.java`](../../src/main/java/com/eopis/order/service/OrderPricingService.java):

```java
BigDecimal subtotal = BigDecimal.ZERO;
for (OrderItem item : items) {
    BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    subtotal.add(itemTotal); // ⚠️ DEFECT: Return value is discarded!
}
```

Because the return value of `subtotal.add(itemTotal)` was never assigned back to `subtotal`, the variable remained bound to `BigDecimal.ZERO` through every iteration.

> [!CAUTION]
> Calling any arithmetic method on `BigDecimal`, `BigInteger`, `String`, `LocalDate`, or `Instant` as an independent statement (without assigning its return value) is always a silent no-op.

---

## Why It Happened

1. **Mental Model Confusion**: Engineers accustomed to primitive accumulator idioms (such as `subtotal += itemTotal;`) or mutable collection builders (such as `list.add(item)`) frequently misjudge `BigDecimal.add()` as a mutating operation.
2. **Missing Compiler / Linter Guardrails**: Standard javac does not treat discarded return values from pure methods as errors unless configured with static analysis tools like Error Prone or SonarLint with `@CheckReturnValue`.

---

## Proof & Verification

### Automated Test Failure

```text
[ERROR] Failures: 
[ERROR]   OrderPricingServiceTest.shouldCalculateCorrectPricingForVipCustomerOrder:48 
expected: <1300.00>
 but was: <0.00>
```

### Debugger State Inspection

```text
================================================================================
Variables View at breakpoint (OrderPricingService.java:41):
================================================================================
item      = {OrderItem@4892} "[Product: PROD-381, Qty: 1, Price: 1200.00]"
itemTotal = {BigDecimal@4893} "1200.00"
subtotal  = {BigDecimal@4870} "0.00"
================================================================================
After Stepping Over (F8):
subtotal  = {BigDecimal@4870} "0.00"  <-- PROOF: State did not update!
================================================================================
Evaluate Expression (Alt+F8): "subtotal.add(itemTotal)"
Result    = {BigDecimal@4910} "1200.00" <-- PROOF: Method works; assignment was omitted!
================================================================================
```

---

## IntelliJ IDEA Step-by-Step Debugging Walkthrough

Follow these steps in IntelliJ IDEA to recreate and verify the debugging process:

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                       IntelliJ IDEA Debug Session Flow                      │
│                                                                             │
│  [OrderPricingServiceTest]                                                  │
│        │                                                                    │
│        ▼ (Shift+F9)                                                         │
│  Line 37: Breakpoint hit ──► Step Into (F7)                                 │
│                                    │                                        │
│                                    ▼                                        │
│                        [OrderPricingService.java]                           │
│                        Line 40: itemTotal calculated                        │
│                        Line 41: subtotal.add(itemTotal)                     │
│                                    │                                        │
│                                    ▼ (F8 Step Over)                         │
│                        Inspect Variables View:                              │
│                        • subtotal is STILL 0.00                             │
│                                    │                                        │
│                                    ▼ (Alt+F8 Evaluate Expression)           │
│                        Evaluate: subtotal.add(itemTotal)                    │
│                        Result: 1200.00                                      │
│                        ==> Root Cause Proved!                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

1. **Set Breakpoint**: Open [`OrderPricingServiceTest.java`](../../src/test/java/com/eopis/order/service/OrderPricingServiceTest.java) and click in the left gutter at line 37 on the invocation:
   ```java
   OrderPricingSummary summary = pricingService.calculateOrderPricing(customer, items, null);
   ```
2. **Launch Debugger**: Press `Shift+F9` (or click the green bug icon next to the test class).
3. **Step Into (`F7`)**: When the execution suspends at line 37, press **`F7` (Step Into)** to enter `OrderPricingService.calculateOrderPricing`.
4. **Step Over (`F8`)**: Step down into the `for` loop body. Observe `itemTotal` evaluate to `1200.00`.
5. **Inspect Variables**: Examine the **Variables View** in the bottom panel:
   - `itemTotal`: `1200.00`
   - `subtotal`: `0.00`
6. **Step Over the Add Statement**: Press `F8` to advance past `subtotal.add(itemTotal);`. Observe that `subtotal` remains `0.00` and is not highlighted in blue (no state mutation occurred).
7. **Evaluate Expression (`Alt+F8`)**:
   - Press **`Alt+F8`** to open the Evaluate Expression dialogue.
   - Enter: `subtotal.add(itemTotal)`.
   - Click **Evaluate**.
   - Notice the result is `{BigDecimal} 1200.00`. This provides conclusive proof that the method functions properly, but the calling code discarded the returned instance.

---

## Remediation & Code Fix

Reassign the returned instance back to the `subtotal` reference, or use Java 8+ Stream functional reduction:

### Imperative Fix

```diff
  BigDecimal subtotal = BigDecimal.ZERO;
  for (OrderItem item : items) {
      BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
-     subtotal.add(itemTotal);
+     subtotal = subtotal.add(itemTotal);
  }
```

### Functional Stream Fix (Recommended)

```java
BigDecimal subtotal = items.stream()
        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

> [!TIP]
> The functional stream approach using `.reduce(BigDecimal.ZERO, BigDecimal::add)` eliminates mutable local variable state entirely, rendering this category of defect structurally impossible.

---

## Regression Testing

Run the test suite to ensure the fix resolves the failure and prevents future regressions:

```bash
# macOS / Linux
./mvnw test -Dtest=OrderPricingServiceTest

# Windows (PowerShell)
.\mvnw.cmd test -Dtest=OrderPricingServiceTest
```

The test `shouldCalculateCorrectPricingForVipCustomerOrder` verifies:
- Cumulative subtotal across multiple items: `$1,200.00 + $100.00 = $1,300.00`
- VIP customer tier discount calculation (10% discount: `-$130.00`)
- Tax computation (8% sales tax applied to discounted subtotal)
- Free shipping threshold evaluation (subtotal > $100.00 qualifies for free shipping)
- Final order total precision rounded to 2 decimal places with `RoundingMode.HALF_UP`

**Output:**
```text
[INFO] Running com.eopis.order.service.OrderPricingServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.142 s -- in com.eopis.order.service.OrderPricingServiceTest
[INFO] BUILD SUCCESS
```

---

## Architectural Takeaways & Defensive Engineering

1. **Immutable API Design Principles**:
   - Always remember that `BigDecimal`, `BigInteger`, `String`, `UUID`, and all classes in `java.time.*` (`Instant`, `LocalDate`, `Duration`) are immutable value objects.
   - Any transformation method returns a brand-new instance.
2. **Static Analysis Enforcement**:
   - Annotate value-object calculation utilities with `@CheckReturnValue` (JSR-305 / Error Prone).
   - Configure compiler flags (`-Werror` or Error Prone check `CheckReturnValue`) in Maven `pom.xml` to fail the build whenever a pure return value is discarded.
3. **Prefer Functional Reduction**:
   - Use `Stream.reduce()` or custom `Collector` implementations for monetary accumulation to prevent manual accumulator re-assignment defects.

---

## Related Resources

- **Scored Rubric Report**: [`REPORT-001-bigdecimal-subtotal.md`](../debugging-reports/REPORT-001-bigdecimal-subtotal.md)
- **Target Source Code**: [`src/main/java/com/eopis/order/service/OrderPricingService.java`](../../src/main/java/com/eopis/order/service/OrderPricingService.java)
- **Target Unit Test**: [`src/test/java/com/eopis/order/service/OrderPricingServiceTest.java`](../../src/test/java/com/eopis/order/service/OrderPricingServiceTest.java)
- **System Architecture**: [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md)
