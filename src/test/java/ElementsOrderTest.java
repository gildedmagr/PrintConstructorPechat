import org.junit.Assert;
import org.junit.Test;
import ru.pechat55.constructor.render.Element2D;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

public class ElementsOrderTest extends ConstructorTest {

    @Test
    public void test() {
        constructor.driver.manage().timeouts().scriptTimeout(Duration.of(1, SECONDS));

        Element2D rect1 = constructor.addRectangle();
        Element2D rect2 = constructor.addRectangle();
        Element2D rect3 = constructor.addRectangle();

        String state = constructor.getState();

        Assert.assertEquals(0, rect1.getIndex());
        Assert.assertEquals(1, rect2.getIndex());
        Assert.assertEquals(2, rect3.getIndex());

        rect1.toFront();

        Assert.assertNotEquals(constructor.getState(), state);

        Assert.assertEquals(2, rect1.getIndex());
        Assert.assertEquals(0, rect2.getIndex());
        Assert.assertEquals(1, rect3.getIndex());

    }
}
