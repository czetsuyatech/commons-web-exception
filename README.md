# Czetsuya Tech | Commons Web Exception

This project is meant to be used to provide common exception handling across multiple microservices.

## Class Diagram

![Class Diagram](docs/img/class-diagram.png).

## Usage

There are several classes that we need to define in order to setup and use this library.

But before we do that it's important to know the prefixes that this project is using in defining
exceptions.

- `A` - application error
- `B` - business error
- `S` - system error
- `E` - entity error

1. Define the service's exception codes as enum. For example, in one of the services I have in
   Hivemaster, a custom Keycloak project that provides multi-tenant feature.

```
@Getter
public enum AppExceptionCodes {

  USER_CREATION_FAILED("A1001", "Use creation failed"),
  USER_EID_NOT_FOUND("A1002", "User EID not found"),
  USER_EMAIL_NOT_FOUND("A1003", "User email not found"),
  USER_PHONE_NOT_FOUND("A1004", "User phone not found"),
  ORGANIZATION_NOT_FOUND("A1005", "Organization not found");

  private String code;
  private String message;

  AppExceptionCodes(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public static Map<String, String> getMapValues() {

    Map<String, String> map = new LinkedHashMap<>();
    for (AppExceptionCodes errCode : values()) {
      map.put(errCode.getCode(), errCode.getDesc());
    }

    return map;
  }
}
```

2. Register the exception codes by extending the class AbstractWebExceptionCodes, so that they can
   be accessed by the commons-exception project.

```
@Component
public class WebExceptions extends AbstractWebExceptions {

  @Value("${spring.application.name}")
  private String serviceName;

  public WebExceptions() {

    super(HttpStatus.OK);

    registerExceptionMap(AppExceptionCodes.getMapValues());
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }
}
```

3. Extend the WebBaseException class. So that we can handle business exceptions specific to the
   service. This class extends ErrorResponseException and RuntimeException which should be extended
   by the service exception classes. This will allow us to override the decoration that happens on
   the base WebBaseException class.

```
public class WebException extends WebBaseException {

  public WebException(HttpStatusCode status, String code) {
    this(status, code, null);
  }

  public WebException(HttpStatusCode status, String code, String message) {
    super(status, code, message);
  }
}
```

4. Extend the base exception handler AbstractWebExceptionHandler, which provides custom error
   handling and decoration for exceptions like method argument, runtime, invalid format, etc.

```
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class WebExceptionHandler extends AbstractWebExceptionHandler {

  private final WebExceptions webExceptions;

  @Override
  public String getServiceName() {
    return webExceptions.getServiceName();
  }
}
```

5. And finally, import the library in your project.

```
@EnableConfigurationProperties
@SpringBootApplication
@Import({WebExceptionHandlerConfig.class})
public class Application {}
```

6. Example Client

If you want to check how this library is used, you can clone the client repository
at https://github.com/czetsuya/commons-web-exception-client.
