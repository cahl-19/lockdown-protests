/*
 * This File is Part of LDProtest
 * Copyright (C) 2021 Covid Anti Hysterics League
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package ldprotest.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class UnitTestReflectionTools {


    private final int testField = 0;

    @Test
    /**
     * This test is to confirm my understanding of Java's reflection API. It  has no other benefit.
     */
    public void testFieldModifierChangeEffectOnOtherFields() throws IllegalArgumentException, IllegalAccessException {

        Field f1 = ReflectionTools.getExistingField(UnitTestReflectionTools.class, "testField");
        Field f2 = ReflectionTools.getExistingField(UnitTestReflectionTools.class, "testField");

        Field modifiers = ReflectionTools.getExistingField(Field.class, "modifiers");
        modifiers.setAccessible(true);

        int mod1 = f1.getModifiers();
        int mod2 = f2.getModifiers();

        assertEquals(mod1, mod2);

        modifiers.setInt(f1, Modifier.PUBLIC);
        f1.setAccessible(true);

        assertEquals(f2.getModifiers(), mod2);
        assertNotEquals(f1.getModifiers(), mod1);

        assertEquals(0, f1.get(this));

        f1.setInt(this, 1);

        assertEquals(1, f1.get(this));

        IllegalAccessException e = assertThrows(IllegalAccessException.class, () -> f2.setInt(this, 2));
        assertNotNull(e);

        assertEquals(1, f2.get(this));
    }
}
