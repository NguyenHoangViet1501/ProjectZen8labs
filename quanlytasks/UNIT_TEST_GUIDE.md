# Hướng dẫn chạy Unit Tests

## Yêu cầu
- Java 17+
- Maven 3.6+

## Chạy Unit Tests

### 1. Chạy tất cả tests
```bash
cd d:\ProjectZen8labs\quanlytasks
mvn test
```

### 2. Chạy tests với coverage report
```bash
mvn test jacoco:report
```

### 3. Chạy test cho một class cụ thể
```bash
mvn test -Dtest=AuthServiceImplTest
mvn test -Dtest=TaskServiceImplTest
mvn test -Dtest=SubTaskServiceImplTest
mvn test -Dtest=CommentServiceImplTest
mvn test -Dtest=NotificationServiceImplTest
mvn test -Dtest=TaskHistoryServiceImplTest
```

### 4. Chạy test cho một method cụ thể
```bash
mvn test -Dtest=AuthServiceImplTest#login_Success
```

## Xem Coverage Report

Sau khi chạy `mvn test`, mở file sau trong trình duyệt:
```
target/site/jacoco/index.html
```

## Cấu trúc Test Files

```
src/test/java/com/backend/quanlytasks/
├── QuanlytasksApplicationTests.java    # Context load test
└── service/
    ├── AuthServiceImplTest.java        # 11 tests
    ├── TaskServiceImplTest.java        # 18 tests
    ├── SubTaskServiceImplTest.java     # 14 tests
    ├── CommentServiceImplTest.java     # 7 tests
    ├── NotificationServiceImplTest.java # 11 tests
    └── TaskHistoryServiceImplTest.java  # 8 tests

src/test/resources/
└── application-test.properties          # Test configuration
```

## Kết quả mong đợi
- **Tổng tests:** 68+
- **Failures:** 0
- **Coverage:** > 50%
