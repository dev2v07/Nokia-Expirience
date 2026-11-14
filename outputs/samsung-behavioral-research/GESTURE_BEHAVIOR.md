# Gesture Behavior

| Сценарий | Samsung | Nokia Exp 2 | Статус |
|---|---|---|---|
| Tap | OBSERVED (ADB) | запуск через существующий resolver проверен ADB | VERIFIED |
| Long press icon | NOT_VERIFIED | engine использует системный long-press timeout | REPLICATED |
| Swipe page | OBSERVED (ADB) | синтетическая проверка выполнена, точная страница требует повторения с фиксированным исходным экраном | VERIFIED |
| Long press + drag | NOT_VERIFIED | физический pointer sequence не проверен | NOT_VERIFIED |
| Cancel | NOT_VERIFIED | чистая transaction-модель и cancel реализованы | REPLICATED |

ADB `input swipe` не считается физическим Drag & Drop.
