## Process and Modify Java Source files

Transforms an enum declaration into an expandable enum that conforms to [Azure Guidelines for Java Enumeration](https://azure.github.io/azure-sdk/java_introduction.html#enumerations).

Example: 

```java
public enum CustomEnum {
    CASE_1,
    CASE_2
}
```
Would be transformed into:

```java
import com.azure.android.core.util.ExpandableStringEnum;

public final class CustomEnum extends ExpandableStringEnum<CustomEnum> {

    public static CustomEnum CASE_1 = fromString("CASE_1");

    public static CustomEnum CASE_2 = fromString("CASE_2");

    /**
     * Creates or finds a {@link CustomEnum} from its string representation.
     * @param name a name to look for.
     * @return the corresponding {@link CustomEnum}
     */
    public static CustomEnum fromString(String name) {
        int ordinal = CustomEnum.findOrdinalByName(name);
        return fromString(name, CustomEnum.class).setMOrdinal(ordinal);
    }

    private int mOrdinal = 0;

    private CustomEnum setMOrdinal(int mOrdinal) {
        this.mOrdinal = mOrdinal;
        return this;
    }

    private int ordinal() {
        return mOrdinal;
    }

    private static int findOrdinalByName(String name) {
        switch(name) {
            case "CASE_1":
                return 0;
            case "CASE_2":
                return 1;
            default:
                throw new RuntimeException("Cannot get the ordinal from string");
        }
    }
}

```

Running with `--source-dirs /dir/to/sources/ --configuration /path/to/test-config.json`

Where configuration is:
```json
{
    "extensible_enums" : [
        "CustomEnum"
    ]
}
```