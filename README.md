# Kapeta Spring Boot SDK

Java SDK using Spring Boot for interacting with Kapeta Runtime

Provides standard health check and openapi documentation.

Will also resolve configuration for the current block based on the Kapeta Runtime.

Is needed for all Kapeta Java Spring based services.

## Configuration

Kapeta provides a number of default implementations in addition to 
springs normal defaults.

You can see the full list of defaults in 
the [KapetaDefaultConfig](src/main/java/com/kapeta/spring/config/KapetaDefaultConfig.java) class.

Note that all of them will be silently overridden if you provide your own implementation.

### OpenAPI Redirect
Kapeta automatically redirects the root path ```GET /``` to the openapi documentation.

To disable this behaviour add the following to your application.yml

This is needed if you want to use the root path for other purposes.

```yaml
kapeta:
  docs:
    redirect: false
``` 

### Kapeta Routes
Kapeta provides a number of standard routes for health checks and metrics.
It is not recommended to disable these routes as they are used by 
kapeta and will cause issues.

You disable them using the following configuration.

```yaml
kapeta:
    routes:
        enabled: false
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details