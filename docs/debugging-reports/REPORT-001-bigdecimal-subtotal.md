# Debugging Report: BUG-001 (BigDecimal Immutable Subtotal Calculation)

- **Bug ID**: `BUG-001`
- **Module**: `order` / `OrderPricingService`
- **Difficulty**: Level 1 (Fundamentals)
- **Time to Detect**: < 10 mins
- **Status**: SOLVED

---

### Evaluation & Rubric Scoring

| Evaluation Dimension | Weight | Score (1-5) | Notes |
| :--- | :--- | :--- | :--- |
| **Systematic Isolation** | 25% | 5/5 | Isolated via unit test `OrderPricingServiceTest` without noise. |
| **IntelliJ Debugger Utilization** | 25% | 5/5 | Line breakpoint inside stream accumulation loop, verified `subtotal.add(...)` return value discarding. |
| **Root Cause Depth** | 25% | 5/5 | Identified `java.math.BigDecimal` immutability contract. |
| **Remediation Quality** | 25% | 5/5 | Replaced discarding loop with functional `reduce(BigDecimal.ZERO, BigDecimal::add)`. |

**Overall Score**: **100% (Pass)**

---

### Root Cause & Fix Reference
See detailed analysis in [`docs/bugs/BUG-001-immutable-bigdecimal-subtotal.md`](../bugs/BUG-001-immutable-bigdecimal-subtotal.md).
