package extensions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 23/06/2018
 */
@ExtendWith(MockitoExtension.class)
final class SampleTest {

  /** Map mock. */
  private final Map mMap = mock(Map.class);

  /* Calculator mock. */
  //@InjectMocks private final MyClass mCalculator = new MyClass(mMap);

  /** Setup test. */
  @BeforeEach final void setUp() {
    System.out.println("MyClassTest.setUp");
    
  }

  /** Reset test. */
  @AfterEach final void tearDown() {
    System.out.println("MyClassTest.tearDown");
  }

  @Test final void sum() {
    when(mMap.size()).thenReturn(3);

    //System.out.println("MyClassTest.sum " + mCalculator.sum(1, 2));

    //System.out.println("MyClassTest.sum " + mCalculator.getCode(new Bundle()));

  }
}