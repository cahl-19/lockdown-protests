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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ReflectionTools {

    private static final Field MODIFIER_FIELD = getExistingField(Field.class, "modifiers");
    private static final int ACCESSIBILITY_BIT_MASK = (Modifier.FINAL | Modifier.PRIVATE | Modifier.PROTECTED);

    static {
        MODIFIER_FIELD.setAccessible(true);
    }

    private ReflectionTools() {
        /* do not construct */
    }


    public static <T> T construct(Class<T> clazz)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        int modifiers = constructor.getModifiers();

        if(modifiersInaccessible(modifiers)) {
            try {
                constructor.setAccessible(true);
                return constructor.newInstance();
            } finally {
                constructor.setAccessible(false);
            }
        } else {
            return constructor.newInstance();
        }
    }

    public static List<Type> getGenericTypeParameters(Field f) {
        return getGenericTypeParameters((ParameterizedType)f.getGenericType());
    }

    public static List<Type> getGenericTypeParameters(ParameterizedType type) {
        return Arrays.asList(type.getActualTypeArguments());
    }

    public static void fieldAccessibleContext(
        Field f, FieldAction action
    ) throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException {

        int modifiers = f.getModifiers();

        if(modifiersInaccessible(modifiers)) {
            try {
                f.setAccessible(true);
                MODIFIER_FIELD.setInt(f, (modifiers & ~ACCESSIBILITY_BIT_MASK));
                action.act();
            } finally {
                MODIFIER_FIELD.setInt(f, modifiers);
                f.setAccessible(false);
            }
        } else {
            action.act();
        }
    }

    public static Collection<Field> instanceFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        for(Field f: clazz.getDeclaredFields())  {
            if(!Modifier.isStatic(f.getModifiers())) {
                fields.add(f);
            }
        }

        return fields;
    }

    private static boolean modifiersAccessible(int modifiers) {
        return (modifiers & ACCESSIBILITY_BIT_MASK) == 0;
    }

    private static boolean modifiersInaccessible(int modifiers) {
        return !modifiersAccessible(modifiers);
    }

    private static Field getExistingField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(String.format("Field %s was expected to exist.", name));
        }
    }

    @FunctionalInterface
    public interface FieldAction {
        void act(
            ) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException;
    }
}
