/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.validation.hibernate;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;

/**
 * A driver.
 */
public class Driver extends Person {
    @Min(value = 18, message = "You have to be 18 to drive a car", groups = DriverChecks.class)
    public int age;

    @AssertTrue(message = "You first have to pass the driving test", groups = DriverChecks.class)
    public boolean hasDrivingLicense;

    public Driver(String name) {
        super( name );
    }

    public void passedDrivingTest(boolean b) {
        hasDrivingLicense = b;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
