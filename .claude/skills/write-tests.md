# 테스트 작성 스킬

## 단위 테스트 (Service)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("ID로 사용자 조회 - 존재하는 경우")
    void findById_존재하는ID_유저반환() {
        // given
        User user = User.builder().email("test@test.com").name("테스트").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserResponse result = userService.findById(1L);

        // then
        assertThat(result.email()).isEqualTo("test@test.com");
        then(userRepository).should().findById(1L);
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 없는 경우 예외")
    void findById_없는ID_예외발생() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findById(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
```

## 단위 테스트 (Controller)

```java
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/users/{id} - 성공")
    void findById_성공() throws Exception {
        // given
        UserResponse response = new UserResponse(1L, "test@test.com", "테스트", LocalDateTime.now());
        given(userService.findById(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }
}
```

## 규칙
- 테스트 메서드명: `메서드명_조건_기대결과` (한글 가능)
- given-when-then 패턴 필수
- `@DisplayName`으로 한글 설명
- Junit 사용 (`assertThat`, `assertThatThrownBy`)
- BDDMockito 사용 (`given`, `then`, `willReturn`)
