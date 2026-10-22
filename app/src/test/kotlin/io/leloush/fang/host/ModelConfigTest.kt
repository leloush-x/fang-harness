package io.leloush.fang.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether a model costs nothing: an id the provider marks free, or a price of zero.
 */
class ModelConfigTest {
  @Test
  fun anIdMarkedFreeIsFree() {
    assertTrue(ModelConfig(id = "deepseek-v4.1-flash:free").isFree())
  }

  @Test
  fun aModelWithNoPriceIsFree() {
    assertTrue(ModelConfig(id = "gpt-oss-120b", cost = ModelCost(0.0, 0.0, 0.0, 0.0)).isFree())
  }

  @Test
  fun aModelThatCostsIsNotFree() {
    val price = ModelCost(input = 0.15, output = 0.6, cacheRead = 0.0, cacheWrite = 0.0)
    assertFalse(ModelConfig(id = "gpt-5", cost = price).isFree())
  }

  @Test
  fun aModelWithNoPriceAtAllIsNotKnownToBeFree() {
    assertFalse(ModelConfig(id = "some-model").isFree())
  }

  @Test
  fun aFreeIdIsFreeWhateverThePriceSays() {
    val price = ModelCost(input = 0.15, output = 0.6, cacheRead = 0.0, cacheWrite = 0.0)
    assertTrue(ModelConfig(id = "a-model:free", cost = price).isFree())
  }
}
