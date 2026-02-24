# 기능 구현 스킬

새로운 기능을 구현할 때 이 순서를 따른다.

## 구현 순서

1. **Entity 생성** (필요한 경우)
   - `BaseEntity` 상속
   - `@Entity`, `@Table(name = "테이블명")` 추가
   - 필드에 적절한 JPA 어노테이션 (`@Column`, `@Enumerated` 등)
   - Lombok: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Builder`

2. **Repository 생성**
   - `JpaRepository<Entity, Long>` 상속
   - 커스텀 쿼리는 `@Query` 또는 메서드 이름 규칙 사용
   - 복잡한 쿼리는 QueryDSL 대신 JPQL 사용

3. **DTO 생성**
   - Java `record` 사용 권장
   - Request/Response 분리: `CreateUserRequest`, `UserResponse`
   - 검증: `@NotBlank`, `@Email`, `@Size` 등

4. **Service 생성**
   - `@Service`, `@RequiredArgsConstructor`, `@Slf4j`, `@Transactional(readOnly = true)`
   - 읽기 메서드: 클래스 레벨 `@Transactional(readOnly = true)` 상속
   - 쓰기 메서드: `@Transactional` 오버라이드
   - 에러: `throw new BusinessException(ErrorCode.XXX)`

5. **Controller 생성**
   - `@RestController`, `@RequestMapping("/api/xxx")`, `@RequiredArgsConstructor`
   - 응답: `ResponseEntity<ApiResponse<T>>` 반환
   - 입력 검증: `@Valid @RequestBody`
   - 경로 변수: `@PathVariable`

6. **테스트 작성**
   - Service 단위 테스트: `@ExtendWith(MockitoExtension.class)`
   - Controller 단위 테스트: `@WebMvcTest`
   - 통합 테스트: `@SpringBootTest` (필요 시만)

## 코드 예시

```java
// Entity
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;
}

// DTO (record)
public record CreateUserRequest(
    @NotBlank String email,
    @NotBlank String name
) {}

public record UserResponse(
    Long id,
    String email,
    String name,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}

// Service
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        // ...
    }
}

// Controller
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }
}
```
