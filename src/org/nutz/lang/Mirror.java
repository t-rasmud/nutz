package org.nutz.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nutz.castor.Castors;
import org.nutz.castor.FailToCastObjectException;
import org.nutz.lang.born.BornContext;
import org.nutz.lang.born.Borning;
import org.nutz.lang.born.BorningException;
import org.nutz.lang.born.Borns;
import org.nutz.lang.eject.EjectByField;
import org.nutz.lang.eject.EjectByGetter;
import org.nutz.lang.eject.Ejecting;
import org.nutz.lang.inject.InjectByField;
import org.nutz.lang.inject.InjectBySetter;
import org.nutz.lang.inject.Injecting;
import org.nutz.lang.util.Callback;
import org.nutz.lang.util.Callback3;

/**
 * 
 * @author zozoh(zozohtnt@gmail.com)
 * 
 * @param <T>
 */
public class Mirror<T> {

    private static class DefaultTypeExtractor implements TypeExtractor {

        public Class<?>[] extract(Mirror<?> mirror) {
            Class<?> theType = mirror.getType();
            List<Class<?>> re = new ArrayList<Class<?>>(5);

            if (theType.isPrimitive()) {
                re.add(mirror.getWrapperClass());
                if (theType != boolean.class && theType != char.class) {
                    re.add(Number.class);
                }
            }
            else if (mirror.isOf(Calendar.class)) {
                re.add(Calendar.class);
            }
            else {
                re.add(theType);
                if (mirror.klass.isEnum()) {
                    re.add(Enum.class);
                }
                else if (mirror.klass.isArray()) {
                    re.add(Array.class);
                }
                else if (mirror.isStringLike())
                    re.add(CharSequence.class);
                else if (mirror.isNumber()) {
                    re.add(Number.class);
                }
                // Map
                else if (mirror.isOf(Map.class)) {
                    re.add(Map.class);
                }
                else if (mirror.isOf(List.class)) {
                    re.add(List.class);
                    re.add(Collection.class);
                }
                else if (mirror.isOf(Collection.class)) {
                    re.add(Collection.class);
                }
            }
            if (theType != Object.class)
                re.add(Object.class);

            return re.toArray(new Class<?>[re.size()]);
        }

    }

    private final static DefaultTypeExtractor defaultTypeExtractor = new DefaultTypeExtractor();

    /**
     * 
     * @param classOfT
     * @return Mirror
     */
    public static <T> Mirror<T> me(Class<T> classOfT) {
        return null == classOfT ? null
                                : new Mirror<T>(classOfT).setTypeExtractor(defaultTypeExtractor);
    }

    /**
     * 
     * @param obj
     */
    @SuppressWarnings("unchecked")
    public static <T> Mirror<T> me(T obj) {
        if (obj == null)
            return null;
        if (obj instanceof Class<?>)
            return (Mirror<T>) me((Class<?>) obj);
        return (Mirror<T>) me(obj.getClass());
    }

    /**
     * 
     * @param classOfT
     * @param typeExtractor
     * @return Mirror
     * @see org.nutz.lang.TypeExtractor
     */
    public static <T> Mirror<T> me(Class<T> classOfT, TypeExtractor typeExtractor) {
        return null == classOfT ? null
                                : new Mirror<T>(classOfT).setTypeExtractor(typeExtractor == null ? defaultTypeExtractor
                                                                                                 : typeExtractor);
    }

    /**
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Mirror<T> me(Type type) {
        if (null == type) {
            return null;
        }
        Mirror<T> mir = (Mirror<T>) Mirror.me(Lang.getTypeClass(type));
        mir.type = type;
        return mir;
    }

    private Class<T> klass;

    private Type type;

    private TypeExtractor typeExtractor;

    /**
     * 
     * @param typeExtractor
     * @return Mirror
     * @see org.nutz.lang.TypeExtractor
     */
    public Mirror<T> setTypeExtractor(TypeExtractor typeExtractor) {
        this.typeExtractor = typeExtractor;
        return this;
    }

    private Mirror(Class<T> classOfT) {
        klass = classOfT;
    }

    /**
     * <p>
     * 
     * @param fieldName
     * @throws NoSuchMethodException
     */
    public Method getGetter(String fieldName) throws NoSuchMethodException {
        return getGetter(fieldName, null);
    }

    /**
     * <p>
     * 
     * @param fieldName
     * @param returnType
     * @throws NoSuchMethodException
     */
    public Method getGetter(String fieldName, Class<?> returnType) throws NoSuchMethodException {
        String fn = Strings.upperFirst(fieldName);
        String _get = "get" + fn;
        String _is = "is" + fn;
        Method _m = null;
        for (Method method : klass.getMethods()) {
            if (method.getParameterTypes().length != 0)
                continue;

            Class<?> mrt = method.getReturnType();

            if (null == mrt)
                continue;

            if (null != returnType && !returnType.equals(mrt))
                continue;

            if (!method.isAccessible())
                method.setAccessible(true);

            if (_get.equals(method.getName()))
                return method;

            if (_is.equals(method.getName())) {
                if (!Mirror.me(mrt).isBoolean())
                    throw new NoSuchMethodException();
                return method;
            }

            if (fieldName.equals(method.getName())) {
                _m = method;
                continue;
            }
        }
        if (_m != null)
            return _m;
        throw Lang.makeThrow(NoSuchMethodException.class,
                             "Fail to find getter for [%s]->[%s]",
                             klass.getName(),
                             fieldName);
    }

    /**
     * <p>
     * 
     * @param field
     * @throws NoSuchMethodException
     */
    public Method getGetter(Field field) throws NoSuchMethodException {
        return getGetter(field.getName(), field.getType());
    }

    /**
     * <p>
     * 
     * <pre>
     * </pre>
     * 
     * <ul>
     * </ul>
     * 
     * @param method
     * @param callback
     * @param whenError
     */
    public static void evalGetterSetter(Method method,
                                        Callback3<String, Method, Method> callback,
                                        Callback<Method> whenError) {
        String name = method.getName();
        Method getter = null;
        Method setter = null;

        if (name.startsWith("get") && method.getParameterTypes().length == 0) {
            name = Strings.lowerFirst(name.substring(3));
            getter = method;
            try {
                setter = method.getDeclaringClass().getMethod("set"
                                                              + Strings.upperFirst(name),
                                                              method.getReturnType());
            }
            catch (Exception e) {}

        }
        else if (name.startsWith("is")
                 && Mirror.me(method.getReturnType()).isBoolean()
                 && method.getParameterTypes().length == 0) {
            name = Strings.lowerFirst(name.substring(2));
            getter = method;
            try {
                setter = method.getDeclaringClass().getMethod("set"
                                                              + Strings.upperFirst(name),
                                                              method.getReturnType());
            }
            catch (Exception e) {}
        }
        else if (name.startsWith("set") && method.getParameterTypes().length == 1) {
            name = Strings.lowerFirst(name.substring(3));
            setter = method;
            try {
                getter = method.getDeclaringClass().getMethod("get" + Strings.upperFirst(name));
            }
            catch (Exception e) {}

        }
        else {
            if (null != whenError)
                whenError.invoke(method);
            return;
        }
        if (null != callback)
            callback.invoke(name, getter, setter);
    }

    /**
     * 
     * @param method
     * @param errmsgFormat
     * @param callback
     */
    public static void evalGetterSetter(final Method method,
                                        final String errmsgFormat,
                                        Callback3<String, Method, Method> callback) {
        evalGetterSetter(method, callback, new Callback<Method>() {
            public void invoke(Method method) {
                throw Lang.makeThrow(errmsgFormat,
                                     method.getName(),
                                     method.getDeclaringClass().getName());
            }
        });
    }

    /**
     * <p>
     * 
     * @param field
     * @throws NoSuchMethodException
     */
    public Method getSetter(Field field) throws NoSuchMethodException {
        return getSetter(field.getName(), field.getType());
    }

    /**
     * 
     * @param fieldName
     * @param paramType
     * @throws NoSuchMethodException
     */
    public Method getSetter(String fieldName, Class<?> paramType) throws NoSuchMethodException {
        try {
            String setterName = "set" + Strings.upperFirst(fieldName);
            try {
                return klass.getMethod(setterName, paramType);
            }
            catch (Throwable e) {
                try {
                    return klass.getMethod(fieldName, paramType);
                }
                catch (Throwable e1) {
                    Mirror<?> type = Mirror.me(paramType);
                    for (Method method : klass.getMethods()) {
                        if (method.getParameterTypes().length == 1)
                            if (method.getName().equals(setterName)
                                || method.getName().equals(fieldName)) {
                                if (null == paramType
                                    || type.canCastToDirectly(method.getParameterTypes()[0]))
                                    return method;
                            }
                    }
                    if (!paramType.isPrimitive()) {
                        Class<?> p = unWrapper();
                        if (null != p)
                            return getSetter(fieldName, p);
                    }
                    throw new RuntimeException();
                }
            }
        }
        catch (Throwable e) {
            throw Lang.makeThrow(NoSuchMethodException.class,
                                 "Fail to find setter for [%s]->[%s(%s)]",
                                 klass.getName(),
                                 fieldName,
                                 paramType == null ? "" : paramType.getName());
        }
    }

    /**
     * 
     * @param fieldName
     */
    public Method[] findSetters(String fieldName) {
        String mName = "set" + Strings.upperFirst(fieldName);
        List<Method> ms = new ArrayList<Method>();
        for (Method m : this.klass.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())
                && m.getParameterTypes().length == 1
                && m.getName().equals(mName))
                ms.add(m);
        }
        return ms.toArray(new Method[ms.size()]);
    }

    /**
     * 
     * @param name
     * @throws NoSuchFieldException
     */
    public Field getField(String name) throws NoSuchFieldException {
        Class<?> cc = klass;
        while (null != cc && cc != Object.class) {
            try {
                return cc.getDeclaredField(name);
            }
            catch (NoSuchFieldException e) {
                cc = cc.getSuperclass();
            }
        }
        throw new NoSuchFieldException(String.format("Can NOT find field [%s] in class [%s] and it's parents classes",
                                                     name,
                                                     klass.getName()));
    }

    /**
     * 
     * @param ann
     * @throws NoSuchFieldException
     */
    public <AT extends Annotation> Field getField(Class<AT> ann) throws NoSuchFieldException {
        for (Field field : this.getFields()) {
            if (field.isAnnotationPresent(ann))
                return field;
        }
        throw new NoSuchFieldException(String.format("Can NOT find field [@%s] in class [%s] and it's parents classes",
                                                     ann.getName(),
                                                     klass.getName()));
    }

    /**
     * 
     * @param ann
     */
    public <AT extends Annotation> Field[] getFields(Class<AT> ann) {
        List<Field> fields = new LinkedList<Field>();
        for (Field f : this.getFields()) {
            if (f.isAnnotationPresent(ann))
                fields.add(f);
        }
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * 
     */
    public Field[] getFields() {
        return _getFields(true, false, true, true);
    }

    /**
     * 
     * @param noFinal
     * 
     */
    public Field[] getStaticField(boolean noFinal) {
        return _getFields(false, true, noFinal, true);
    }

    private Field[] _getFields(boolean noStatic,
                               boolean noMember,
                               boolean noFinal,
                               boolean noInner) {
        Class<?> cc = klass;
        Map<String, Field> map = new LinkedHashMap<String, Field>();
        while (null != cc && cc != Object.class) {
            Field[] fs = cc.getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                Field f = fs[i];
                int m = f.getModifiers();
                if (noStatic && Modifier.isStatic(m))
                    continue;
                if (noFinal && Modifier.isFinal(m))
                    continue;
                if (noInner && f.getName().startsWith("this$"))
                    continue;
                if (noMember && !Modifier.isStatic(m))
                    continue;
                if (map.containsKey(fs[i].getName()))
                    continue;

                map.put(fs[i].getName(), fs[i]);
            }
            cc = cc.getSuperclass();
        }
        return map.values().toArray(new Field[map.size()]);
    }

    /**
     * 
     * @param <A>
     * @param annType
     */
    public <A extends Annotation> A getAnnotation(Class<A> annType) {
        Class<?> cc = klass;
        A ann;
        do {
            ann = cc.getAnnotation(annType);
            cc = cc.getSuperclass();
        } while (null == ann && cc != Object.class);
        return ann;
    }

    /**
     */
    public Type[] getGenericsTypes() {
        if (type instanceof ParameterizedType) {
            return Lang.getGenericsTypes(type);
        }
        return null;
    }

    /**
     */
    public Type getGenericsType(int index) {
        Type[] ts = getGenericsTypes();
        return ts == null ? null : (ts.length <= index ? null : ts[index]);
    }

    /**
     */
    public Method[] getMethods() {
        Class<?> cc = klass;
        List<Method> list = new LinkedList<Method>();
        while (null != cc && cc != Object.class) {
            Method[] ms = cc.getDeclaredMethods();
            for (int i = 0; i < ms.length; i++) {
                list.add(ms[i]);
            }
            cc = cc.getSuperclass();
        }
        return list.toArray(new Method[list.size()]);
    }

    /**
     * <p>
     * 
     * @param top
     */
    public Method[] getAllDeclaredMethods(Class<?> top) {
        Class<?> cc = klass;
        Map<String, Method> map = new LinkedHashMap<String, Method>();
        while (null != cc && cc != Object.class) {
            Method[] fs = cc.getDeclaredMethods();
            for (int i = 0; i < fs.length; i++) {
                String key = fs[i].getName() + Mirror.getParamDescriptor(fs[i].getParameterTypes());
                if (!map.containsKey(key))
                    map.put(key, fs[i]);
            }
            cc = cc.getSuperclass() == top ? null : cc.getSuperclass();
        }
        return map.values().toArray(new Method[map.size()]);
    }

    /**
     * 
     */
    public Method[] getAllDeclaredMethodsWithoutTop() {
        return getAllDeclaredMethods(Object.class);
    }

    /**
     */
    public Method[] getStaticMethods() {
        List<Method> list = new LinkedList<Method>();
        for (Method m : klass.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()))
                list.add(m);
        }
        return list.toArray(new Method[list.size()]);
    }

    private static RuntimeException makeSetValueException(Class<?> type,
                                                          String name,
                                                          Object value,
                                                          Exception e) {
        if (e instanceof FailToSetValueException) {
            return (FailToSetValueException) e;
        }
        return new FailToSetValueException(String.format("Fail to set value [%s] to [%s]->[%s] because '%s'",
                                                         value,
                                                         type.getName(),
                                                         name,
                                                         e.getMessage()),
                                           e);
    }

    /**
     * 
     * @param obj
     * @param field
     * @param value
     * @throws FailToSetValueException
     */
    public void setValue(Object obj, Field field, Object value) throws FailToSetValueException {
        if (!field.isAccessible())
            field.setAccessible(true);
        Class<?> ft = field.getType();
        if (null != value) {
            if (!field.getType().isAssignableFrom(value.getClass()))
                try {
                    value = Castors.me().castTo(value, field.getType());
                }
                catch (FailToCastObjectException e) {
                    throw makeSetValueException(obj.getClass(), field.getName(), value, e);
                }
        }
        else if (ft.isPrimitive()) {
            if (boolean.class == ft) {
                value = false;
            } else if (char.class == ft) {
                value = (char) 0;
            } else {
                value = (byte) 0;
            }
        }
        try {
            this.getSetter(field).invoke(obj, value);
        }
        catch (Exception e1) {
            try {
                field.set(obj, value);
            }
            catch (Exception e) {
                throw makeSetValueException(obj.getClass(), field.getName(), value, e);
            }
        }
    }

    /**
     * 
     * @param obj
     * @param fieldName
     * @param value
     * @throws FailToSetValueException
     */
    public void setValue(Object obj, String fieldName, Object value)
            throws FailToSetValueException {
        if (null == value) {
            try {
                setValue(obj, this.getField(fieldName), null);
            }
            catch (Exception e1) {
                throw makeSetValueException(obj.getClass(), fieldName, null, e1);
            }
        } else {
            try {
                this.getSetter(fieldName, value.getClass()).invoke(obj, value);
            }
            catch (Exception e) {
                try {
                    setValue(obj, this.getField(fieldName), value);
                }
                catch (Exception e1) {
                    throw makeSetValueException(obj.getClass(), fieldName, value, e1);
                }
            }
        }
    }

    private static RuntimeException makeGetValueException(Class<?> type, String name, Throwable e) {
        return new FailToGetValueException(String.format("Fail to get value for [%s]->[%s]",
                                                         type.getName(),
                                                         name),
                                           e);
    }

    /**
     * 
     * @param obj
     * @param f
     * @throws FailToGetValueException
     */
    public Object getValue(Object obj, Field f) throws FailToGetValueException {
        if (!f.isAccessible())
            f.setAccessible(true);
        try {
            return f.get(obj);
        }
        catch (Exception e) {
            throw makeGetValueException(obj.getClass(), f.getName(), e);
        }
    }

    /**
     * 
     * @param obj
     * @param name
     * @throws FailToGetValueException
     */
    @SuppressWarnings("rawtypes")
    public Object getValue(Object obj, String name) throws FailToGetValueException {
        try {
            return this.getGetter(name).invoke(obj);
        }
        catch (Exception e) {
            try {
                return getValue(obj, getField(name));
            }
            catch (NoSuchFieldException e1) {
                if (obj != null) {
                    if (obj.getClass().isArray() && "length".equals(name)) {
                        return Lang.eleSize(obj);
                    }
                    if (obj instanceof Map) {
                        return ((Map)obj).get(name);
                    }
                    if (obj instanceof List) {
                        try {
                            return ((List)obj).get(Integer.parseInt(name));
                        }
                        catch (Exception e2) {
                        }
                    }
                }
                throw makeGetValueException(obj == null ? getType() : obj.getClass(), name, e);
            }
        }
    }

    /**
     */
    public Class<T> getType() {
        return klass;
    }

    private String _type_id;

    /**
     */
    public String getTypeId() {
        if (null == _type_id) {
            if (null != type && type instanceof ParameterizedType) {
                ParameterizedType pmType = (ParameterizedType) type;
                List<Type> list = new ArrayList<Type>(pmType.getActualTypeArguments().length);
                for (Type pmA : pmType.getActualTypeArguments()) {
                    list.add(pmA);
                }
                _type_id = String.format("%s<%s>", klass.getName(), Lang.concat(",", list));
            }
            else {
                _type_id = klass.getName();
            }
            _type_id += "_" + klass.getClassLoader();
        }
        return _type_id;
    }

    /**
     */
    public Type getActuallyType() {
        return type == null ? klass : type;
    }

    /**
     */
    public Class<?>[] extractTypes() {
        return typeExtractor.extract(this);
    }

    /**
     * 
     * @throws RuntimeException
     */
    public Class<?> getWrapperClass() {
        if (!klass.isPrimitive()) {
            if (this.isPrimitiveNumber() || this.is(Boolean.class) || this.is(Character.class))
                return klass;
            if (Number.class.isAssignableFrom(klass))
                return klass;
            throw Lang.makeThrow("Class '%s' should be a primitive class", klass.getName());
        }
        if (is(int.class))
            return Integer.class;
        if (is(char.class))
            return Character.class;
        if (is(boolean.class))
            return Boolean.class;
        if (is(long.class))
            return Long.class;
        if (is(float.class))
            return Float.class;
        if (is(byte.class))
            return Byte.class;
        if (is(short.class))
            return Short.class;
        if (is(double.class))
            return Double.class;

        throw Lang.makeThrow("Class [%s] has no wrapper class!", klass.getName());
    }

    /**
     */
    public Class<?> getWrapper() {
        if (klass.isPrimitive())
            return getWrapperClass();
        return klass;
    }

    /**
     */
    public Class<?> getOuterClass() {
        if (Modifier.isStatic(klass.getModifiers()))
            return null;
        String name = klass.getName();
        int pos = name.lastIndexOf('$');
        if (pos == -1)
            return null;
        name = name.substring(0, pos);
        try {
            return Lang.loadClass(name);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 
     * @param args
     * 
     * @throws BorningException
     * 
     * @see org.nutz.lang.born.Borning
     */
    public Borning<T> getBorning(Object... args) throws BorningException {
        BornContext<T> bc = Borns.eval(klass, args);
        if (null == bc)
            throw new BorningException(klass, args);

        return bc.getBorning();
    }

    /**
     * 
     * @param argTypes
     * 
     * @throws BorningException
     * @throws NullPointerException
     *             when args is null
     */
    public Borning<T> getBorningByArgTypes(Class<?>... argTypes) throws BorningException {
        BornContext<T> bc = Borns.evalByArgTypes(klass, argTypes);
        if (null == bc)
            throw new BorningException(klass, argTypes);
        return bc.getBorning();
    }

    /**
     * 
     * @param args
     */
    public T born(Object... args) {
        BornContext<T> bc = Borns.eval(klass, args);
        if (null == bc)
            throw new BorningException(klass, args);

        return bc.doBorn();
    }

    private static boolean doMatchMethodParamsType(Class<?>[] paramTypes,
                                                   Class<?>[] methodArgTypes) {
        if (paramTypes.length == 0 && methodArgTypes.length == 0)
            return true;
        if (paramTypes.length == methodArgTypes.length) {
            for (int i = 0; i < paramTypes.length; i++)
                if (!Mirror.me(paramTypes[i]).canCastToDirectly((methodArgTypes[i])))
                    return false;
            return true;
        } else if (paramTypes.length + 1 == methodArgTypes.length) {
            if (!methodArgTypes[paramTypes.length].isArray())
                return false;
            for (int i = 0; i < paramTypes.length; i++)
                if (!Mirror.me(paramTypes[i]).canCastToDirectly((methodArgTypes[i])))
                    return false;
            return true;
        }
        return false;
    }

    /**
     * 
     * @param methodName
     * @param args
     */
    public Invoking getInvoking(String methodName, Object... args) {
        return new Invoking(klass, methodName, args);
    }

    /**
     * 
     * @param fieldName
     */
    public Injecting getInjecting(String fieldName) {
        Method[] sss = this.findSetters(fieldName);
        if (sss.length == 1)
            return new InjectBySetter(sss[0]);
        else
            try {
                Field field = this.getField(fieldName);
                try {
                    return new InjectBySetter(this.getSetter(field));
                }
                catch (NoSuchMethodException e) {
                    return new InjectByField(field);
                }
            }
            catch (NoSuchFieldException e) {
                throw Lang.wrapThrow(e);
            }
    }

    /**
     * 
     * @param fieldName
     */
    public Ejecting getEjecting(String fieldName) {
        return getEjecting(fieldName, null);
    }

    public Ejecting getEjecting(Field field) {
        try {
            return new EjectByGetter(getGetter(field));
        }
        catch (NoSuchMethodException e1) {
            return new EjectByField(field);
        }
    }

    public Ejecting getEjecting(String fieldName, Class<?> returnType) {
        try {
            return new EjectByGetter(getGetter(fieldName, returnType));
        }
        catch (NoSuchMethodException e) {
            try {
                Field field = this.getField(fieldName);
                return getEjecting(field);
            }
            catch (NoSuchFieldException e1) {
                throw Lang.wrapThrow(e1);
            }
        }

    }

    /**
     * 
     * @param obj
     * @param methodName
     * @param args
     */
    public Object invoke(Object obj, String methodName, Object... args) {
        return getInvoking(methodName, args).invoke(obj);
    }

    /**
     * 
     * @param name
     * @param args
     */
    public Method findMethod(String name, Object[] args) throws NoSuchMethodException {
        if (null == args || args.length == 0)
            return findMethod(name);
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++)
            paramTypes[i] = args[i].getClass();
        return findMethod(name, paramTypes);
    }

    /**
     * 
     * @param name
     * @param paramTypes
     */
    public Method findMethod(String name, Class<?>... paramTypes) throws NoSuchMethodException {
        try {
            return klass.getMethod(name, paramTypes);
        }
        catch (NoSuchMethodException e) {
            for (Method m : klass.getMethods()) {
                if (m.getName().equals(name))
                    if (doMatchMethodParamsType(paramTypes, m.getParameterTypes()))
                        return m;
            }
        }
        Method[] sms = getMethods();
        OUT: for (Method m : sms) {
            if (!m.getName().equals(name))
                continue;
            Class<?>[] pts = m.getParameterTypes();
            if (pts.length == 1 && pts[0].isArray()) {
                if (paramTypes.length == 0)
                    return m;
                Class<?> varParam = pts[0].getComponentType();
                for (Class<?> klass : paramTypes) {
                    if (!Castors.me().canCast(klass, varParam))
                        continue OUT;
                }
                return m;
            }
        }
        throw new NoSuchMethodException(String.format("Fail to find Method %s->%s with params:\n%s",
                                                      klass.getName(),
                                                      name,
                                                      Castors.me().castToString(paramTypes)));
    }

    /**
     * 
     * @param name
     * @param argNumber
     */
    public Method[] findMethods(String name, int argNumber) {
        List<Method> methods = new LinkedList<Method>();
        for (Method m : klass.getMethods())
            if (m.getName().equals(name))
                if (argNumber < 0)
                    methods.add(m);
                else if (m.getParameterTypes().length == argNumber)
                    methods.add(m);
        return methods.toArray(new Method[methods.size()]);
    }

    /**
     * 
     * @param returnType
     * @param paramTypes
     */
    public Method findMethod(Class<?> returnType, Class<?>... paramTypes)
            throws NoSuchMethodException {
        for (Method m : klass.getMethods()) {
            if (returnType == m.getReturnType())
                if (paramTypes.length == m.getParameterTypes().length) {
                    boolean noThisOne = false;
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] != m.getParameterTypes()[i]) {
                            noThisOne = true;
                            break;
                        }
                    }
                    if (!noThisOne)
                        return m;
                }
        }
        throw new NoSuchMethodException(String.format("Can not find method in [%s] with return type '%s' and arguemtns \n'%s'!",
                                                      klass.getName(),
                                                      returnType.getName(),
                                                      Castors.me().castToString(paramTypes)));

    }

    /**
     * 
     * @param methodParamTypes
     * @param args
     * 
     * @see org.nutz.lang.MatchType
     */
    public static MatchType matchParamTypes(Class<?>[] methodParamTypes, Object... args) {
        return matchParamTypes(methodParamTypes, evalToTypes(args));
    }

    /**
     * 
     * @param args
     */
    public static Class<?>[] evalToTypes(Object... args) {
        Class<?>[] types = new Class[args.length];
        int i = 0;
        for (Object arg : args)
            types[i++] = null == arg ? Object.class : arg.getClass();
        return types;
    }

    public static Object evalArgToSameTypeRealArray(Object... args) {
        Object array = evalArgToRealArray(args);
        return array == args ? null : array;
    }

    /**
     * 
     * @param args
     */
    public static Object evalArgToRealArray(Object... args) {
        if (null == args || args.length == 0 || null == args[0])
            return null;
        Object re = null;
        /*
         * Check inside the arguments list, to see if all element is in same
         * type
         */
        Class<?> type = null;
        for (Object arg : args) {
            if (null == arg)
                break;
            if (null == type) {
                type = arg.getClass();
                continue;
            }
            if (arg.getClass() != type) {
                type = null;
                break;
            }
        }
        /*
         * If all argument elements in same type, make a new Array by the Type
         */
        if (type != null) {
            re = Array.newInstance(type, args.length);
            for (int i = 0; i < args.length; i++) {
                Array.set(re, i, args[i]);
            }
            return re;
        }
        return args;

    }

    /**
     * 
     * @param paramTypes
     * @param argTypes
     * 
     * @see org.nutz.lang.MatchType
     */
    public static MatchType matchParamTypes(Class<?>[] paramTypes, Class<?>[] argTypes) {
        int len = argTypes == null ? 0 : argTypes.length;
        if (len == 0 && paramTypes.length == 0)
            return MatchType.YES;
        if (paramTypes.length == len) {
            for (int i = 0; i < len; i++)
                if (!Mirror.me(argTypes[i]).canCastToDirectly((paramTypes[i])))
                    return MatchType.NO;
            return MatchType.YES;
        } else if (len + 1 == paramTypes.length) {
            if (!paramTypes[len].isArray())
                return MatchType.NO;
            for (int i = 0; i < len; i++)
                if (!Mirror.me(argTypes[i]).canCastToDirectly((paramTypes[i])))
                    return MatchType.NO;
            return MatchType.LACK;
        }
        return MatchType.NO;
    }

    /**
     * 
     * @param type
     */
    public boolean is(Class<?> type) {
        return null != type && klass == type;
    }

    /**
     * 
     * @param className
     */
    public boolean is(String className) {
        return klass.getName().equals(className);
    }

    /**
     * @param type
     */
    public boolean isOf(Class<?> type) {
        return type.isAssignableFrom(klass);
    }

    /**
     */
    public boolean isString() {
        return is(String.class);
    }

    /**
     */
    public boolean isStringLike() {
        return CharSequence.class.isAssignableFrom(klass);
    }

    /**
     */
    public boolean isSimple() {
        return isStringLike()
               || isBoolean()
               || isChar()
               || isNumber()
               || isDateTimeLike()
               || isEnum();
    }

    /**
     */
    public boolean isChar() {
        return is(char.class) || is(Character.class);
    }

    /**
     */
    public boolean isEnum() {
        return klass.isEnum();
    }

    /**
     */
    public boolean isBoolean() {
        return is(boolean.class) || is(Boolean.class);
    }

    /**
     */
    public boolean isFloat() {
        return is(float.class) || is(Float.class);
    }

    /**
     */
    public boolean isDouble() {
        return is(double.class) || is(Double.class);
    }

    /**
     */
    public boolean isInt() {
        return is(int.class) || is(Integer.class);
    }

    /**
     */
    public boolean isIntLike() {
        return isInt() || isLong() || isShort() || isByte() || is(BigDecimal.class);
    }

    /**
     */
    public boolean isInterface() {
        return klass.isInterface();
    }

    /**
     */
    public boolean isDecimal() {
        return isFloat() || isDouble();
    }

    /**
     */
    public boolean isLong() {
        return is(long.class) || is(Long.class);
    }

    /**
     */
    public boolean isShort() {
        return is(short.class) || is(Short.class);
    }

    /**
     */
    public boolean isByte() {
        return is(byte.class) || is(Byte.class);
    }

    /**
     * @param type
     */
    public boolean isWrapperOf(Class<?> type) {
        try {
            return Mirror.me(type).getWrapperClass() == klass;
        }
        catch (Exception e) {}
        return false;
    }

    /**
     * @param type
     */
    public boolean canCastToDirectly(Class<?> type) {
        if (klass == type || type.isAssignableFrom(klass))
            return true;
        if (klass.isPrimitive() && type.isPrimitive()) {
            if (this.isPrimitiveNumber() && Mirror.me(type).isPrimitiveNumber())
                return true;
        }
        try {
            return Mirror.me(type).getWrapperClass() == this.getWrapperClass();
        }
        catch (Exception e) {}
        return false;
    }

    /**
     */
    public boolean isPrimitiveNumber() {
        return isInt() || isLong() || isFloat() || isDouble() || isByte() || isShort();
    }

    /**
     * 
     * @return true or false
     */
    public boolean isObj() {
        return isContainer() || isPojo();
    }

    /**
     * <ul>
     * </ul>
     * 
     * @return true or false
     */
    public boolean isPojo() {
        if (this.klass.isPrimitive() || this.isEnum())
            return false;

        if (this.isStringLike() || this.isDateTimeLike())
            return false;

        if (this.isPrimitiveNumber() || this.isBoolean() || this.isChar())
            return false;

        return !isContainer();
    }

    /**
     * 
     * @return true of false
     */
    public boolean isContainer() {
        return isColl() || isMap();
    }

    /**
     * 
     * @return true of false
     */
    public boolean isArray() {
        return klass.isArray();
    }

    /**
     * 
     * @return true of false
     */
    public boolean isCollection() {
        return isOf(Collection.class);
    }

    /**
     */
    public boolean isColl() {
        return isArray() || isCollection();
    }

    /**
     * 
     * @return true of false
     */
    public boolean isMap() {
        return isOf(Map.class);
    }

    /**
     */
    public boolean isNumber() {
        return Number.class.isAssignableFrom(klass)
               || klass.isPrimitive() && !is(boolean.class) && !is(char.class);
    }

    /**
     */
    public boolean isDateTimeLike() {
        return Calendar.class.isAssignableFrom(klass)
               || java.util.Date.class.isAssignableFrom(klass)
               || java.sql.Date.class.isAssignableFrom(klass)
               || java.sql.Time.class.isAssignableFrom(klass);
    }

    public String toString() {
        return klass.getName();
    }

    /**
     * 
     * @param pts
     */
    public static Object blankArrayArg(Class<?>[] pts) {
        return Array.newInstance(pts[pts.length - 1].getComponentType(), 0);
    }

    /**
     */
    public static Type[] getTypeParams(Class<?> klass) {
        if (klass == null || "java.lang.Object".equals(klass.getName()))
            return null;
        Type superclass = klass.getGenericSuperclass();
        if (null != superclass && superclass instanceof ParameterizedType)
            return ((ParameterizedType) superclass).getActualTypeArguments();

        Type[] interfaces = klass.getGenericInterfaces();
        for (Type inf : interfaces) {
            if (inf instanceof ParameterizedType) {
                return ((ParameterizedType) inf).getActualTypeArguments();
            }
        }
        return getTypeParams(klass.getSuperclass());
    }

    private static final Pattern PTN = Pattern.compile("(<)(.+)(>)");

    /**
     * 
     * @param f
     */
    public static Class<?>[] getGenericTypes(Field f) {
        String gts = f.toGenericString();
        Matcher m = PTN.matcher(gts);
        if (m.find()) {
            String s = m.group(2);
            String[] ss = Strings.splitIgnoreBlank(s);
            if (ss.length > 0) {
                Class<?>[] re = new Class<?>[ss.length];
                try {
                    for (int i = 0; i < ss.length; i++) {
                        String className = ss[i];
                        if (className.length() > 0 && className.charAt(0) == '?')
                            re[i] = Object.class;
                        else {
                            int pos = className.indexOf('<');
                            if (pos < 0)
                                re[i] = Lang.loadClass(className);
                            else
                                re[i] = Lang.loadClass(className.substring(0, pos));
                        }
                    }
                    return re;
                }
                catch (ClassNotFoundException e) {
                    throw Lang.wrapThrow(e);
                }
            }
        }
        return new Class<?>[0];
    }

    /**
     * 
     * @param f
     */
    public static Class<?> getGenericTypes(Field f, int index) {
        Class<?>[] types = getGenericTypes(f);
        if (null == types || types.length <= index)
            return null;
        return types[index];
    }

    /**
     * 
     * @param klass
     * @param index
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getTypeParam(Class<?> klass, int index) {
        Type[] types = getTypeParams(klass);
        if (types == null)
            return null;
        if (index >= 0 && index < types.length) {
            Type t = types[index];
            Class<T> clazz = (Class<T>) Lang.getTypeClass(t);
            if (clazz == null)
                throw Lang.makeThrow("Type '%s' is not a Class", t.toString());
            return clazz;
        }
        throw Lang.makeThrow("Class type param out of range %d/%d", index, types.length);
    }

    /**
     * @param klass
     */
    public static String getPath(Class<?> klass) {
        return klass.getName().replace('.', '/');
    }

    /**
     * @param parameterTypes
     */
    public static String getParamDescriptor(Class<?>[] parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Class<?> pt : parameterTypes)
            sb.append(getTypeDescriptor(pt));
        sb.append(')');
        return sb.toString();
    }

    /**
     * @param method
     */
    public static String getMethodDescriptor(Method method) {
        return getParamDescriptor(method.getParameterTypes())
               + getTypeDescriptor(method.getReturnType());
    }

    /**
     * @param c
     */
    public static String getConstructorDescriptor(Constructor<?> c) {
        return getParamDescriptor(c.getParameterTypes()) + "V";
    }

    /**
     * @param klass
     */
    public static String getTypeDescriptor(Class<?> klass) {
        if (klass.isPrimitive()) {
            if (klass == void.class)
                return "V";
            else if (klass == int.class)
                return "I";
            else if (klass == long.class)
                return "J";
            else if (klass == byte.class)
                return "B";
            else if (klass == short.class)
                return "S";
            else if (klass == float.class)
                return "F";
            else if (klass == double.class)
                return "D";
            else if (klass == char.class)
                return "C";
            else
                /* if(klass == boolean.class) */
                return "Z";
        }
        StringBuilder sb = new StringBuilder();
        if (klass.isArray()) {
            return sb.append('[').append(getTypeDescriptor(klass.getComponentType())).toString();
        }
        return sb.append('L').append(Mirror.getPath(klass)).append(';').toString();
    }

    /**
     * 
     * @param type
     * @param ann
     */
    public static Field findField(Class<?> type, Class<? extends Annotation> ann) {
        Mirror<?> mirror = Mirror.me(type);
        for (Field f : mirror.getFields())
            if (f.isAnnotationPresent(ann))
                return f;
        return null;
    }

    public Class<?> unWrapper() {
        return TypeMapping2.get(klass);
    }

    private static final Map<Class<?>, Class<?>> TypeMapping2 = new HashMap<Class<?>, Class<?>>();

    static {

        TypeMapping2.put(Short.class, short.class);
        TypeMapping2.put(Integer.class, int.class);
        TypeMapping2.put(Long.class, long.class);
        TypeMapping2.put(Double.class, double.class);
        TypeMapping2.put(Float.class, float.class);
        TypeMapping2.put(Byte.class, byte.class);
        TypeMapping2.put(Character.class, char.class);
        TypeMapping2.put(Boolean.class, boolean.class);
    }

    /**
     * 
     * 
     * @param clzInterface
     */
    public boolean hasInterface(Class<?> clzInterface) {
        Class<?>[] interfaces = klass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (clzInterface.equals(interfaces[i])) {
                return true;
            }
        }
        return false;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Annotation> T getAnnotationDeep(Method method, Class<T> annotationClass) {
        T t = method.getAnnotation(annotationClass);
        if (t != null)
            return t;
        Class klass = method.getDeclaringClass().getSuperclass();
        while (klass != null && klass != Object.class) {
            try {
                for (Method m : klass.getMethods()) {
                    if (m.getName().equals(method.getName())) {
                        Class[] mParameters = m.getParameterTypes();
                        Class[] methodParameters = method.getParameterTypes();
                        if (mParameters.length != methodParameters.length)
                            continue;
                        boolean match = true;
                        for (int i = 0; i < mParameters.length; i++) {
                            if (!mParameters[i].isAssignableFrom(methodParameters[i])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            t = m.getAnnotation(annotationClass);
                            if (t != null)
                                return t;
                        }
                    }
                }
            }
            catch (Exception e) {
                break;
            }
            klass = klass.getSuperclass();
        }
        for (Class klass2 : method.getDeclaringClass().getInterfaces()) {
            try {
                Method tmp = klass2.getMethod(method.getName(), method.getParameterTypes());
                t = tmp.getAnnotation(annotationClass);
                if (t != null)
                    return t;
            }
            catch (Exception e) {}
        }
        return null;
    }

    public static <T extends Annotation> T getAnnotationDeep(Class<?> type, Class<T> annotationClass) {
        T t = type.getAnnotation(annotationClass);
        if (t != null)
            return t;
        Class<?> klass = type.getSuperclass();
        while (klass != null && klass != Object.class) {
            try {
                t = klass.getAnnotation(annotationClass);
                if (t != null)
                    return t;
            }
            catch (Exception e) {
                break;
            }
            klass = klass.getSuperclass();
        }
        for (Class<?> klass2 : type.getInterfaces()) {
            try {
                t = klass2.getAnnotation(annotationClass);
                if (t != null)
                    return t;
            }
            catch (Exception e) {}
        }
        return null;
    }

}
