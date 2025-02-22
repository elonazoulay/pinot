/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.predicate;

import java.util.Collections;
import java.util.Random;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.pinot.common.data.FieldSpec;
import org.apache.pinot.common.utils.BytesUtils;
import org.apache.pinot.core.common.predicate.EqPredicate;
import org.apache.pinot.core.common.predicate.NEqPredicate;
import org.apache.pinot.core.operator.filter.predicate.EqualsPredicateEvaluatorFactory;
import org.apache.pinot.core.operator.filter.predicate.NotEqualsPredicateEvaluatorFactory;
import org.apache.pinot.core.operator.filter.predicate.PredicateEvaluator;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Unit test for no-dictionary based Eq and NEq predicate evaluators.
 */
public class NoDictionaryEqualsPredicateEvaluatorsTest {

  private static final String COLUMN_NAME = "column";
  private static final int NUM_MULTI_VALUES = 100;
  private static final int MAX_STRING_LENGTH = 100;
  private Random _random;

  @BeforeClass
  public void setup() {
    _random = new Random();
  }

  @Test
  public void testIntPredicateEvaluators() {
    int intValue = _random.nextInt();
    EqPredicate eqPredicate = new EqPredicate(COLUMN_NAME, Collections.singletonList(Integer.toString(intValue)));
    PredicateEvaluator eqPredicateEvaluator =
        EqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(eqPredicate, FieldSpec.DataType.INT);

    NEqPredicate neqPredicate = new NEqPredicate(COLUMN_NAME, Collections.singletonList(Integer.toString(intValue)));
    PredicateEvaluator neqPredicateEvaluator =
        NotEqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(neqPredicate, FieldSpec.DataType.INT);

    Assert.assertTrue(eqPredicateEvaluator.applySV(intValue));
    Assert.assertFalse(neqPredicateEvaluator.applySV(intValue));

    int[] randomInts = new int[NUM_MULTI_VALUES];
    PredicateEvaluatorTestUtils.fillRandom(randomInts);
    randomInts[_random.nextInt(NUM_MULTI_VALUES)] = intValue;

    Assert.assertTrue(eqPredicateEvaluator.applyMV(randomInts, NUM_MULTI_VALUES, new MutableInt(0)));
    Assert.assertFalse(neqPredicateEvaluator.applyMV(randomInts, NUM_MULTI_VALUES, new MutableInt(0)));

    for (int i = 0; i < 100; i++) {
      int random = _random.nextInt();
      Assert.assertEquals(eqPredicateEvaluator.applySV(random), (random == intValue));
      Assert.assertEquals(neqPredicateEvaluator.applySV(random), (random != intValue));

      PredicateEvaluatorTestUtils.fillRandom(randomInts);
      Assert.assertEquals(eqPredicateEvaluator.applyMV(randomInts, NUM_MULTI_VALUES, new MutableInt(0)),
          ArrayUtils.contains(randomInts, intValue));
      Assert.assertEquals(neqPredicateEvaluator.applyMV(randomInts, NUM_MULTI_VALUES, new MutableInt(0)),
          !ArrayUtils.contains(randomInts, intValue));
    }
  }

  @Test
  public void testLongPredicateEvaluators() {
    long longValue = _random.nextLong();
    EqPredicate eqPredicate = new EqPredicate(COLUMN_NAME, Collections.singletonList(Long.toString(longValue)));
    PredicateEvaluator eqPredicateEvaluator =
        EqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(eqPredicate, FieldSpec.DataType.LONG);

    NEqPredicate neqPredicate = new NEqPredicate(COLUMN_NAME, Collections.singletonList(Long.toString(longValue)));
    PredicateEvaluator neqPredicateEvaluator =
        NotEqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(neqPredicate, FieldSpec.DataType.LONG);

    Assert.assertTrue(eqPredicateEvaluator.applySV(longValue));
    Assert.assertFalse(neqPredicateEvaluator.applySV(longValue));

    long[] randomLongs = new long[NUM_MULTI_VALUES];
    PredicateEvaluatorTestUtils.fillRandom(randomLongs);
    randomLongs[_random.nextInt(NUM_MULTI_VALUES)] = longValue;

    Assert.assertTrue(eqPredicateEvaluator.applyMV(randomLongs, NUM_MULTI_VALUES));
    Assert.assertFalse(neqPredicateEvaluator.applyMV(randomLongs, NUM_MULTI_VALUES));

    for (int i = 0; i < 100; i++) {
      long random = _random.nextLong();
      Assert.assertEquals(eqPredicateEvaluator.applySV(random), (random == longValue));
      Assert.assertEquals(neqPredicateEvaluator.applySV(random), (random != longValue));

      PredicateEvaluatorTestUtils.fillRandom(randomLongs);
      Assert.assertEquals(eqPredicateEvaluator.applyMV(randomLongs, NUM_MULTI_VALUES),
          ArrayUtils.contains(randomLongs, longValue));
      Assert.assertEquals(neqPredicateEvaluator.applyMV(randomLongs, NUM_MULTI_VALUES),
          !ArrayUtils.contains(randomLongs, longValue));
    }
  }

  @Test
  public void testFloatPredicateEvaluators() {
    // FLOAT data type
    float floatValue = _random.nextFloat();
    EqPredicate eqPredicate = new EqPredicate(COLUMN_NAME, Collections.singletonList(Float.toString(floatValue)));
    PredicateEvaluator eqPredicateEvaluator =
        EqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(eqPredicate, FieldSpec.DataType.FLOAT);

    NEqPredicate neqPredicate = new NEqPredicate(COLUMN_NAME, Collections.singletonList(Float.toString(floatValue)));
    PredicateEvaluator neqPredicateEvaluator =
        NotEqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(neqPredicate, FieldSpec.DataType.FLOAT);

    Assert.assertTrue(eqPredicateEvaluator.applySV(floatValue));
    Assert.assertFalse(neqPredicateEvaluator.applySV(floatValue));

    float[] randomFloats = new float[NUM_MULTI_VALUES];
    PredicateEvaluatorTestUtils.fillRandom(randomFloats);
    randomFloats[_random.nextInt(NUM_MULTI_VALUES)] = floatValue;

    Assert.assertTrue(eqPredicateEvaluator.applyMV(randomFloats, NUM_MULTI_VALUES));
    Assert.assertFalse(neqPredicateEvaluator.applyMV(randomFloats, NUM_MULTI_VALUES));

    for (int i = 0; i < 100; i++) {
      float random = _random.nextFloat();
      Assert.assertEquals(eqPredicateEvaluator.applySV(random), (random == floatValue));
      Assert.assertEquals(neqPredicateEvaluator.applySV(random), (random != floatValue));

      PredicateEvaluatorTestUtils.fillRandom(randomFloats);
      Assert.assertEquals(eqPredicateEvaluator.applyMV(randomFloats, NUM_MULTI_VALUES),
          ArrayUtils.contains(randomFloats, floatValue));
      Assert.assertEquals(neqPredicateEvaluator.applyMV(randomFloats, NUM_MULTI_VALUES),
          !ArrayUtils.contains(randomFloats, floatValue));
    }
  }

  @Test
  public void testDoublePredicateEvaluators() {
    double doubleValue = _random.nextDouble();
    EqPredicate eqPredicate = new EqPredicate(COLUMN_NAME, Collections.singletonList(Double.toString(doubleValue)));
    PredicateEvaluator eqPredicateEvaluator =
        EqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(eqPredicate, FieldSpec.DataType.DOUBLE);

    NEqPredicate neqPredicate = new NEqPredicate(COLUMN_NAME, Collections.singletonList(Double.toString(doubleValue)));
    PredicateEvaluator neqPredicateEvaluator =
        NotEqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(neqPredicate, FieldSpec.DataType.DOUBLE);

    Assert.assertTrue(eqPredicateEvaluator.applySV(doubleValue));
    Assert.assertFalse(neqPredicateEvaluator.applySV(doubleValue));

    double[] randomDoubles = new double[NUM_MULTI_VALUES];
    PredicateEvaluatorTestUtils.fillRandom(randomDoubles);
    randomDoubles[_random.nextInt(NUM_MULTI_VALUES)] = doubleValue;

    Assert.assertTrue(eqPredicateEvaluator.applyMV(randomDoubles, NUM_MULTI_VALUES));
    Assert.assertFalse(neqPredicateEvaluator.applyMV(randomDoubles, NUM_MULTI_VALUES));

    for (int i = 0; i < 100; i++) {
      double random = _random.nextDouble();
      Assert.assertEquals(eqPredicateEvaluator.applySV(random), (random == doubleValue));
      Assert.assertEquals(neqPredicateEvaluator.applySV(random), (random != doubleValue));

      PredicateEvaluatorTestUtils.fillRandom(randomDoubles);
      Assert.assertEquals(eqPredicateEvaluator.applyMV(randomDoubles, NUM_MULTI_VALUES),
          ArrayUtils.contains(randomDoubles, doubleValue));
      Assert.assertEquals(neqPredicateEvaluator.applyMV(randomDoubles, NUM_MULTI_VALUES),
          !ArrayUtils.contains(randomDoubles, doubleValue));
    }
  }

  @Test
  public void testStringPredicateEvaluators() {
    String stringValue = RandomStringUtils.random(MAX_STRING_LENGTH);
    EqPredicate eqPredicate = new EqPredicate(COLUMN_NAME, Collections.singletonList(stringValue));
    PredicateEvaluator eqPredicateEvaluator =
        EqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(eqPredicate, FieldSpec.DataType.STRING);

    NEqPredicate neqPredicate = new NEqPredicate(COLUMN_NAME, Collections.singletonList(stringValue));
    PredicateEvaluator neqPredicateEvaluator =
        NotEqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(neqPredicate, FieldSpec.DataType.STRING);

    Assert.assertTrue(eqPredicateEvaluator.applySV(stringValue));
    Assert.assertFalse(neqPredicateEvaluator.applySV(stringValue));

    String[] randomStrings = new String[NUM_MULTI_VALUES];
    PredicateEvaluatorTestUtils.fillRandom(randomStrings, MAX_STRING_LENGTH);
    randomStrings[_random.nextInt(NUM_MULTI_VALUES)] = stringValue;

    Assert.assertTrue(eqPredicateEvaluator.applyMV(randomStrings, NUM_MULTI_VALUES));
    Assert.assertFalse(neqPredicateEvaluator.applyMV(randomStrings, NUM_MULTI_VALUES));

    for (int i = 0; i < 100; i++) {
      String random = RandomStringUtils.random(MAX_STRING_LENGTH);
      Assert.assertEquals(eqPredicateEvaluator.applySV(random), (random.equals(stringValue)));
      Assert.assertEquals(neqPredicateEvaluator.applySV(random), (!random.equals(stringValue)));

      PredicateEvaluatorTestUtils.fillRandom(randomStrings, MAX_STRING_LENGTH);
      Assert.assertEquals(eqPredicateEvaluator.applyMV(randomStrings, NUM_MULTI_VALUES),
          ArrayUtils.contains(randomStrings, stringValue));
      Assert.assertEquals(neqPredicateEvaluator.applyMV(randomStrings, NUM_MULTI_VALUES),
          !ArrayUtils.contains(randomStrings, stringValue));
    }
  }

  @Test
  public void testBytesPredicateEvaluators() {
    byte[] bytesValue = RandomStringUtils.random(MAX_STRING_LENGTH).getBytes();
    String hexStringValue = BytesUtils.toHexString(bytesValue);
    EqPredicate eqPredicate = new EqPredicate(COLUMN_NAME, Collections.singletonList(hexStringValue));
    PredicateEvaluator eqPredicateEvaluator =
        EqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(eqPredicate, FieldSpec.DataType.BYTES);

    NEqPredicate neqPredicate = new NEqPredicate(COLUMN_NAME, Collections.singletonList(hexStringValue));
    PredicateEvaluator neqPredicateEvaluator =
        NotEqualsPredicateEvaluatorFactory.newRawValueBasedEvaluator(neqPredicate, FieldSpec.DataType.BYTES);

    Assert.assertTrue(eqPredicateEvaluator.applySV(bytesValue));
    Assert.assertFalse(neqPredicateEvaluator.applySV(bytesValue));

    byte[][] randomBytesArray = new byte[NUM_MULTI_VALUES][];
    PredicateEvaluatorTestUtils.fillRandom(randomBytesArray, MAX_STRING_LENGTH);
    randomBytesArray[_random.nextInt(NUM_MULTI_VALUES)] = bytesValue;

    Assert.assertTrue(eqPredicateEvaluator.applyMV(randomBytesArray, NUM_MULTI_VALUES));
    Assert.assertFalse(neqPredicateEvaluator.applyMV(randomBytesArray, NUM_MULTI_VALUES));

    for (int i = 0; i < 100; i++) {
      byte[] randomBytes = RandomStringUtils.random(MAX_STRING_LENGTH).getBytes();
      String randomString = BytesUtils.toHexString(randomBytes);
      Assert.assertEquals(eqPredicateEvaluator.applySV(randomBytes), (randomString.equals(hexStringValue)));
      Assert.assertEquals(neqPredicateEvaluator.applySV(randomBytes), (!randomString.equals(hexStringValue)));

      PredicateEvaluatorTestUtils.fillRandom(randomBytesArray, MAX_STRING_LENGTH);
      Assert.assertEquals(eqPredicateEvaluator.applyMV(randomBytesArray, NUM_MULTI_VALUES),
          ArrayUtils.contains(randomBytesArray, hexStringValue));
      Assert.assertEquals(neqPredicateEvaluator.applyMV(randomBytesArray, NUM_MULTI_VALUES),
          !ArrayUtils.contains(randomBytesArray, hexStringValue));
    }
  }
}
