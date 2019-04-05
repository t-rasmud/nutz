package org.nutz.lang;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.nutz.castor.Castors;
import org.nutz.castor.FailToCastObjectException;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.json.Json;
import org.nutz.lang.stream.StringInputStream;
import org.nutz.lang.stream.StringOutputStream;
import org.nutz.lang.stream.StringWriter;
import org.nutz.lang.util.ClassTools;
import org.nutz.lang.util.Context;
import org.nutz.lang.util.NutMap;
import org.nutz.lang.util.NutType;
import org.nutz.lang.util.SimpleContext;

/**
 *
 * @author zozoh(zozohtnt@gmail.com)
 * @author wendal(wendal1985@gmail.com)
 * @author bonyfish(mc02cxj@gmail.com)
 * @author wizzer(wizzer.cn@gmail.com)
 */
public abstract class Lang {

    public static int HASH_BUFF_SIZE = 16 * 1024;

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private static final Pattern IPV6_STD_PATTERN = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6HexCompressedAddress(final String input) {
        return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6Address(final String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }

    public static ComboException comboThrow(Throwable... es) {
        ComboException ce = new ComboException();
        for (Throwable e : es)
            ce.add(e);
        return ce;
    }

    /**
     *
     */
    public static RuntimeException noImplement() {
        return new RuntimeException("Not implement yet!");
    }

    /**
     *
     */
    public static RuntimeException impossible() {
        return new RuntimeException("r u kidding me?! It is impossible!");
    }

    /**
     *
     * @param format
     * @param args
     */
    public static RuntimeException makeThrow(String format, Object... args) {
        return new RuntimeException(String.format(format, args));
    }

    /**
     *
     * @param classOfT
     * @param format
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T makeThrow(Class<T> classOfT,
                                                    String format,
                                                    Object... args) {
        if (classOfT == RuntimeException.class)
            return (T) new RuntimeException(String.format(format, args));
        return Mirror.me(classOfT).born(String.format(format, args));
    }

    /**
     *
     * @param e
     * @param fmt
     * @param args
     */
    public static RuntimeException wrapThrow(Throwable e, String fmt, Object... args) {
        return new RuntimeException(String.format(fmt, args), e);
    }

    /**
     * <p>
     *
     * @param e
     */
    public static RuntimeException wrapThrow(Throwable e) {
        if (e instanceof RuntimeException)
            return (RuntimeException) e;
        if (e instanceof InvocationTargetException)
            return wrapThrow(((InvocationTargetException) e).getTargetException());
        return new RuntimeException(e);
    }

    /**
     *
     * @param e
     * @param wrapper
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T wrapThrow(Throwable e, Class<T> wrapper) {
        if (wrapper.isAssignableFrom(e.getClass()))
            return (T) e;
        return Mirror.me(wrapper).born(e);
    }

    public static Throwable unwrapThrow(Throwable e) {
        if (e == null)
            return null;
        if (e instanceof InvocationTargetException) {
            InvocationTargetException itE = (InvocationTargetException) e;
            if (itE.getTargetException() != null)
                return unwrapThrow(itE.getTargetException());
        }
        if (e instanceof RuntimeException && e.getCause() != null)
            return unwrapThrow(e.getCause());
        return e;
    }

    public static boolean isCauseBy(Throwable e, Class<? extends Throwable> causeType) {
        if (e.getClass() == causeType)
            return true;
        Throwable cause = e.getCause();
        if (null == cause)
            return false;
        return isCauseBy(cause, causeType);
    }

    /**
     * <ul>
     * </ul>
     *
     * @param a0
     * @param a1
     */
    public static boolean equals(Object a0, Object a1) {
        if (a0 == a1)
            return true;

        if (a0 == null && a1 == null)
            return true;

        if (a0 == null || a1 == null)
            return false;

        if (a0.equals(a1))
            return true;

        Mirror<?> mi = Mirror.me(a0);

        if (mi.isSimple() || mi.is(Pattern.class)) {
            return a0.toString().equals(a1.toString());
        }

        if (!a0.getClass().isAssignableFrom(a1.getClass())
            && !a1.getClass().isAssignableFrom(a0.getClass()))
            return false;

        // Map
        if (a0 instanceof Map && a1 instanceof Map) {
            Map<?, ?> m1 = (Map<?, ?>) a0;
            Map<?, ?> m2 = (Map<?, ?>) a1;
            if (m1.size() != m2.size())
                return false;
            for (Entry<?, ?> e : m1.entrySet()) {
                Object key = e.getKey();
                if (!m2.containsKey(key) || !equals(m1.get(key), m2.get(key)))
                    return false;
            }
            return true;
        }
        else if (a0.getClass().isArray() && a1.getClass().isArray()) {
            int len = Array.getLength(a0);
            if (len != Array.getLength(a1))
                return false;
            for (int i = 0; i < len; i++) {
                if (!equals(Array.get(a0, i), Array.get(a1, i)))
                    return false;
            }
            return true;
        }
        else if (a0 instanceof Collection && a1 instanceof Collection) {
            Collection<?> c0 = (Collection<?>) a0;
            Collection<?> c1 = (Collection<?>) a1;
            if (c0.size() != c1.size())
                return false;

            Iterator<?> it0 = c0.iterator();
            Iterator<?> it1 = c1.iterator();

            while (it0.hasNext()) {
                Object o0 = it0.next();
                Object o1 = it1.next();
                if (!equals(o0, o1))
                    return false;
            }

            return true;
        }

        return false;
    }

    /**
     *
     * @param array
     * @param ele
     */
    public static <T> boolean contains(T[] array, T ele) {
        if (null == array)
            return false;
        for (T e : array) {
            if (equals(e, ele))
                return true;
        }
        return false;
    }

    /**
     *
     * @param reader
     */
    public static String readAll(Reader reader) {
        if (!(reader instanceof BufferedReader))
            reader = new BufferedReader(reader);
        try {
            StringBuilder sb = new StringBuilder();

            char[] data = new char[64];
            int len;
            while (true) {
                if ((len = reader.read(data)) == -1)
                    break;
                sb.append(data, 0, len);
            }
            return sb.toString();
        }
        catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
        finally {
            Streams.safeClose(reader);
        }
    }

    /**
     *
     * @param writer
     * @param str
     */
    public static void writeAll(Writer writer, String str) {
        try {
            writer.write(str);
            writer.flush();
        }
        catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
        finally {
            Streams.safeClose(writer);
        }
    }

    /**
     *
     * @param cs
     */
    public static InputStream ins(CharSequence cs) {
        return new StringInputStream(cs);
    }

    /**
     *
     * @param cs
     */
    public static Reader inr(CharSequence cs) {
        return new StringReader(cs.toString());
    }

    /**
     *
     * @param sb
     */
    public static Writer opw(StringBuilder sb) {
        return new StringWriter(sb);
    }

    /**
     *
     * @param sb
     */
    public static StringOutputStream ops(StringBuilder sb) {
        return new StringOutputStream(sb);
    }

    /**
     *
     * <pre>
     * String[] strs = Lang.array("A", "B", "A"); => ["A","B","A"]
     * </pre>
     *
     * @param eles
     */
    public static <T> T[] array(T... eles) {
        return eles;
    }

    /**
     *
     * <pre>
     * String[] strs = Lang.arrayUniq("A","B","A");  => ["A","B"]
     * String[] strs = Lang.arrayUniq();  => null
     * </pre>
     *
     *
     * @param eles
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayUniq(T... eles) {
        if (null == eles || eles.length == 0)
            return null;
        HashSet<T> set = new HashSet<T>(eles.length);
        for (T ele : eles) {
            set.add(ele);
        }
        T[] arr = (T[]) Array.newInstance(eles[0].getClass(), set.size());
        int index = 0;
        for (T ele : eles) {
            if (set.remove(ele))
                Array.set(arr, index++, ele);
        }
        return arr;

    }

    /**
     * <ul>
     * <li>Map
     * </ul>
     *
     * @param obj
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null)
            return true;
        if (obj.getClass().isArray())
            return Array.getLength(obj) == 0;
        if (obj instanceof Collection<?>)
            return ((Collection<?>) obj).isEmpty();
        if (obj instanceof Map<?, ?>)
            return ((Map<?, ?>) obj).isEmpty();
        return false;
    }

    /**
     *
     * @param ary
     */
    public static <T> boolean isEmptyArray(T[] ary) {
        return null == ary || ary.length == 0;
    }

    /**
     *
     * <pre>
     * List&lt;Pet&gt; pets = Lang.list(pet1, pet2, pet3);
     * </pre>
     *
     *
     * @param eles
     */
    public static <T> ArrayList<T> list(T... eles) {
        ArrayList<T> list = new ArrayList<T>(eles.length);
        for (T ele : eles)
            list.add(ele);
        return list;
    }

    /**
     *
     * @param eles
     */
    public static <T> Set<T> set(T... eles) {
        Set<T> set = new HashSet<T>();
        for (T ele : eles)
            set.add(ele);
        return set;
    }

    /**
     *
     * @param arys
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[]... arys) {
        Queue<T> list = new LinkedList<T>();
        for (T[] ary : arys)
            if (null != ary)
                for (T e : ary)
                    if (null != e)
                        list.add(e);
        if (list.isEmpty())
            return null;
        Class<T> type = (Class<T>) list.peek().getClass();
        return list.toArray((T[]) Array.newInstance(type, list.size()));
    }

    /**
     *
     * @param e
     * @param eles
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayFirst(T e, T[] eles) {
        try {
            if (null == eles || eles.length == 0) {
                T[] arr = (T[]) Array.newInstance(e.getClass(), 1);
                arr[0] = e;
                return arr;
            }
            T[] arr = (T[]) Array.newInstance(eles.getClass().getComponentType(), eles.length + 1);
            arr[0] = e;
            for (int i = 0; i < eles.length; i++) {
                arr[i + 1] = eles[i];
            }
            return arr;
        }
        catch (NegativeArraySizeException e1) {
            throw Lang.wrapThrow(e1);
        }
    }

    /**
     *
     * @param e
     * @param eles
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayLast(T[] eles, T e) {
        try {
            if (null == eles || eles.length == 0) {
                T[] arr = (T[]) Array.newInstance(e.getClass(), 1);
                arr[0] = e;
                return arr;
            }
            T[] arr = (T[]) Array.newInstance(eles.getClass().getComponentType(), eles.length + 1);
            for (int i = 0; i < eles.length; i++) {
                arr[i] = eles[i];
            }
            arr[eles.length] = e;
            return arr;
        }
        catch (NegativeArraySizeException e1) {
            throw Lang.wrapThrow(e1);
        }
    }

    /**
     * <p>
     *
     * @param fmt
     * @param objs
     */
    public static <T> StringBuilder concatBy(String fmt, T[] objs) {
        StringBuilder sb = new StringBuilder();
        for (T obj : objs)
            sb.append(String.format(fmt, obj));
        return sb;
    }

    /**
     * <p>
     * <p>
     *
     * @param ptn
     * @param c
     * @param objs
     */
    public static <T> StringBuilder concatBy(String ptn, Object c, T[] objs) {
        StringBuilder sb = new StringBuilder();
        for (T obj : objs)
            sb.append(String.format(ptn, obj)).append(c);
        if (sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        return sb;
    }

    /**
     * <p>
     *
     * @param c
     * @param objs
     */
    public static <T> StringBuilder concat(Object c, T[] objs) {
        StringBuilder sb = new StringBuilder();
        if (null == objs || 0 == objs.length)
            return sb;

        sb.append(objs[0]);
        for (int i = 1; i < objs.length; i++)
            sb.append(c).append(objs[i]);

        return sb;
    }

    /**
     *
     * @param objs
     * @param val
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] without(T[] objs, T val) {
        if (null == objs || objs.length == 0) {
            return objs;
        }
        List<T> list = new ArrayList<T>(objs.length);
        Class<?> eleType = null;
        for (T obj : objs) {
            if (obj == val || (null != obj && null != val && obj.equals(val)))
                continue;
            if (null == eleType && obj != null)
                eleType = obj.getClass();
            list.add(obj);
        }
        if (list.isEmpty()) {
            return (T[]) new Object[0];
        }
        return list.toArray((T[]) Array.newInstance(eleType, list.size()));
    }

    /**
     * <p>
     *
     * @param c
     * @param vals
     */
    public static StringBuilder concat(Object c, long[] vals) {
        StringBuilder sb = new StringBuilder();
        if (null == vals || 0 == vals.length)
            return sb;

        sb.append(vals[0]);
        for (int i = 1; i < vals.length; i++)
            sb.append(c).append(vals[i]);

        return sb;
    }

    /**
     * <p>
     *
     * @param c
     * @param vals
     */
    public static StringBuilder concat(Object c, int[] vals) {
        StringBuilder sb = new StringBuilder();
        if (null == vals || 0 == vals.length)
            return sb;

        sb.append(vals[0]);
        for (int i = 1; i < vals.length; i++)
            sb.append(c).append(vals[i]);

        return sb;
    }

    /**
     * <p>
     *
     * @param offset
     * @param len
     * @param c
     * @param objs
     */
    public static <T> StringBuilder concat(int offset, int len, Object c, T[] objs) {
        StringBuilder sb = new StringBuilder();
        if (null == objs || len < 0 || 0 == objs.length)
            return sb;

        if (offset < objs.length) {
            sb.append(objs[offset]);
            for (int i = 1; i < len && i + offset < objs.length; i++) {
                sb.append(c).append(objs[i + offset]);
            }
        }
        return sb;
    }

    /**
     *
     * @param objs
     */
    public static <T> StringBuilder concat(T[] objs) {
        StringBuilder sb = new StringBuilder();
        for (T e : objs)
            sb.append(e.toString());
        return sb;
    }

    /**
     *
     * @param offset
     * @param len
     * @param array
     */
    public static <T> StringBuilder concat(int offset, int len, T[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(array[i + offset].toString());
        }
        return sb;
    }

    /**
     * <p>
     *
     * @param c
     * @param coll
     */
    public static <T> StringBuilder concat(Object c, Collection<T> coll) {
        StringBuilder sb = new StringBuilder();
        if (null == coll || coll.isEmpty())
            return sb;
        return concat(c, coll.iterator());
    }

    /**
     * <p>
     *
     * @param c
     * @param it
     */
    public static <T> StringBuilder concat(Object c, Iterator<T> it) {
        StringBuilder sb = new StringBuilder();
        if (it == null || !it.hasNext())
            return sb;
        sb.append(it.next());
        while (it.hasNext())
            sb.append(c).append(it.next());
        return sb;
    }

    /**
     *
     * @param <C>
     * @param <T>
     * @param coll
     * @param objss
     */
    public static <C extends Collection<T>, T> C fill(C coll, T[]... objss) {
        for (T[] objs : objss)
            for (T obj : objs)
                coll.add(obj);
        return coll;
    }

    /**
     *
     * @param mapClass
     * @param coll
     * @param keyFieldName
     */
    public static <T extends Map<Object, Object>> T collection2map(Class<T> mapClass,
                                                                   Collection<?> coll,
                                                                   String keyFieldName) {
        if (null == coll)
            return null;
        T map = createMap(mapClass);
        if (coll.size() > 0) {
            Iterator<?> it = coll.iterator();
            Object obj = it.next();
            Mirror<?> mirror = Mirror.me(obj.getClass());
            Object key = mirror.getValue(obj, keyFieldName);
            map.put(key, obj);
            for (; it.hasNext();) {
                obj = it.next();
                key = mirror.getValue(obj, keyFieldName);
                map.put(key, obj);
            }
        }
        return (T) map;
    }

    /**
     *
     * @param col
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> collection2list(Collection<E> col) {
        if (null == col)
            return null;
        if (col.size() == 0)
            return new ArrayList<E>(0);
        Class<E> eleType = (Class<E>) col.iterator().next().getClass();
        return collection2list(col, eleType);
    }

    /**
     *
     * @param col
     * @param eleType
     */
    public static <E> List<E> collection2list(Collection<?> col, Class<E> eleType) {
        if (null == col)
            return null;
        List<E> list = new ArrayList<E>(col.size());
        for (Object obj : col)
            list.add(Castors.me().castTo(obj, eleType));
        return list;
    }

    /**
     *
     * @param coll
     */
    @SuppressWarnings("unchecked")
    public static <E> E[] collection2array(Collection<E> coll) {
        if (null == coll)
            return null;
        if (coll.size() == 0)
            return (E[]) new Object[0];

        Class<E> eleType = (Class<E>) Lang.first(coll).getClass();
        return collection2array(coll, eleType);
    }

    /**
     *
     * @param col
     * @param eleType
     */
    @SuppressWarnings("unchecked")
    public static <E> E[] collection2array(Collection<?> col, Class<E> eleType) {
        if (null == col)
            return null;
        Object re = Array.newInstance(eleType, col.size());
        int i = 0;
        for (Iterator<?> it = col.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (null == obj)
                Array.set(re, i++, null);
            else
                Array.set(re, i++, Castors.me().castTo(obj, eleType));
        }
        return (E[]) re;
    }

    /**
     *
     * @param mapClass
     * @param array
     * @param keyFieldName
     */
    public static <T extends Map<Object, Object>> T array2map(Class<T> mapClass,
                                                              Object array,
                                                              String keyFieldName) {
        if (null == array)
            return null;
        T map = createMap(mapClass);
        int len = Array.getLength(array);
        if (len > 0) {
            Object obj = Array.get(array, 0);
            Mirror<?> mirror = Mirror.me(obj.getClass());
            for (int i = 0; i < len; i++) {
                obj = Array.get(array, i);
                Object key = mirror.getValue(obj, keyFieldName);
                map.put(key, obj);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Map<Object, Object>> T createMap(Class<T> mapClass) {
        T map;
        try {
            map = mapClass.newInstance();
        }
        catch (Exception e) {
            map = (T) new HashMap<Object, Object>();
        }
        if (!mapClass.isAssignableFrom(map.getClass())) {
            throw Lang.makeThrow("Fail to create map [%s]", mapClass.getName());
        }
        return map;
    }

    /**
     *
     * @param array
     *
     * @see org.nutz.castor.Castors
     */
    public static <T> List<T> array2list(T[] array) {
        if (null == array)
            return null;
        List<T> re = new ArrayList<T>(array.length);
        for (T obj : array)
            re.add(obj);
        return re;
    }

    /**
     *
     * @param array
     * @param eleType
     *
     * @see org.nutz.castor.Castors
     */
    public static <T, E> List<E> array2list(Object array, Class<E> eleType) {
        if (null == array)
            return null;
        int len = Array.getLength(array);
        List<E> re = new ArrayList<E>(len);
        for (int i = 0; i < len; i++) {
            Object obj = Array.get(array, i);
            re.add(Castors.me().castTo(obj, eleType));
        }
        return re;
    }

    /**
     *
     * @param array
     * @param eleType
     * @throws FailToCastObjectException
     *
     * @see org.nutz.castor.Castors
     */
    public static Object array2array(Object array, Class<?> eleType)
            throws FailToCastObjectException {
        if (null == array)
            return null;
        int len = Array.getLength(array);
        Object re = Array.newInstance(eleType, len);
        for (int i = 0; i < len; i++) {
            Array.set(re, i, Castors.me().castTo(Array.get(array, i), eleType));
        }
        return re;
    }

    /**
     *
     * @param args
     * @param pts
     * @throws FailToCastObjectException
     *
     * @see org.nutz.castor.Castors
     */
    public static <T> Object[] array2ObjectArray(T[] args, Class<?>[] pts)
            throws FailToCastObjectException {
        if (null == args)
            return null;
        Object[] newArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = Castors.me().castTo(args[i], pts[i]);
        }
        return newArgs;
    }

    /**
     *
     * @param src
     * @param toType
     * @throws FailToCastObjectException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T map2Object(Map<?, ?> src, Class<T> toType)
            throws FailToCastObjectException {
        if (null == toType)
            throw new FailToCastObjectException("target type is Null");
        if (toType == Map.class)
            return (T) src;
        if (Map.class.isAssignableFrom(toType)) {
            Map map;
            try {
                map = (Map) toType.newInstance();
                map.putAll(src);
                return (T) map;
            }
            catch (Exception e) {
                throw new FailToCastObjectException("target type fail to born!", unwrapThrow(e));
            }

        }
        if (toType.isArray())
            return (T) Lang.collection2array(src.values(), toType.getComponentType());
        // List
        if (List.class == toType) {
            return (T) Lang.collection2list(src.values());
        }

        // POJO
        Mirror<T> mirror = Mirror.me(toType);
        T obj = mirror.born();
        for (Field field : mirror.getFields()) {
            Object v = null;
            if (!Lang.isAndroid && field.isAnnotationPresent(Column.class)) {
                String cv = field.getAnnotation(Column.class).value();
                v = src.get(cv);
            }

            if (null == v && src.containsKey(field.getName())) {
                v = src.get(field.getName());
            }

            if (null != v) {
                Class<?> ft = field.getType();
                Object vv = null;
                if (v instanceof Collection) {
                    Collection c = (Collection) v;
                    if (ft.isArray()) {
                        vv = Lang.collection2array(c, ft.getComponentType());
                    }
                    else {
                        Collection newCol;
                        Class eleType = Mirror.getGenericTypes(field, 0);
                        if (ft == List.class) {
                            newCol = new ArrayList(c.size());
                        } else if (ft == Set.class) {
                            newCol = new LinkedHashSet();
                        } else {
                            try {
                                newCol = (Collection) ft.newInstance();
                            }
                            catch (Exception e) {
                                throw Lang.wrapThrow(e);
                            }
                        }
                        for (Object ele : c) {
                            newCol.add(Castors.me().castTo(ele, eleType));
                        }
                        vv = newCol;
                    }
                }
                // Map
                else if (v instanceof Map && Map.class.isAssignableFrom(ft)) {
                    final Map map;
                    if (ft == Map.class) {
                        map = new HashMap();
                    }
                    else {
                        try {
                            map = (Map) ft.newInstance();
                        }
                        catch (Exception e) {
                            throw new FailToCastObjectException("target type fail to born!", e);
                        }
                    }
                    final Class<?> valType = Mirror.getGenericTypes(field, 1);
                    each(v, new Each<Entry>() {
                        public void invoke(int i, Entry en, int length) {
                            map.put(en.getKey(), Castors.me().castTo(en.getValue(), valType));
                        }
                    });
                    vv = map;
                }
                else {
                    vv = Castors.me().castTo(v, ft);
                }
                mirror.setValue(obj, field, vv);
            }
        }
        return obj;
    }

    /**
     *
     * @param str
     */
    public static NutMap map(String str) {
        if (null == str)
            return null;
        str = Strings.trim(str);
        if (!Strings.isEmpty(str)
            && (Strings.isQuoteBy(str, '{', '}') || Strings.isQuoteBy(str, '(', ')'))) {
            return Json.fromJson(NutMap.class, str);
        }
        return Json.fromJson(NutMap.class, "{" + str + "}");
    }

    /**
     *
     *
     * @param obj
     *
     * @param mkc
     * @param recur
     *
     * @see MapKeyConvertor
     */
    @SuppressWarnings("unchecked")
    public static void convertMapKey(Object obj, MapKeyConvertor mkc, boolean recur) {
        // Map
        if (obj instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) obj;
            NutMap map2 = new NutMap();
            for (Map.Entry<String, Object> en : map.entrySet()) {
                String key = en.getKey();
                Object val = en.getValue();

                if (recur)
                    convertMapKey(val, mkc, recur);

                String newKey = mkc.convertKey(key);
                map2.put(newKey, val);
            }
            map.clear();
            map.putAll(map2);
        }
        // Collection
        else if (obj instanceof Collection<?>) {
            for (Object ele : (Collection<?>) obj) {
                convertMapKey(ele, mkc, recur);
            }
        }
        // Array
        else if (obj.getClass().isArray()) {
            for (Object ele : (Object[]) obj) {
                convertMapKey(ele, mkc, recur);
            }
        }
    }

    /**
     *
     * @param key
     * @param v
     */
    public static NutMap map(String key, Object v) {
        return new NutMap().addv(key, v);
    }

    /**
     *
     * @param fmt
     * @param args
     */
    public static NutMap mapf(String fmt, Object... args) {
        return map(String.format(fmt, args));
    }

    /**
     *
     */
    public static Context context() {
        return new SimpleContext();
    }

    /**
     *
     * @param map
     *
     */
    public static Context context(Map<String, Object> map) {
        return new SimpleContext(map);
    }

    /**
     *
     * @param fmt
     * @param args
     *
     */
    public static Context contextf(String fmt, Object... args) {
        return context(Lang.mapf(fmt, args));
    }

    /**
     *
     */
    public static Context context(String str) {
        return context(map(str));
    }

    /**
     *
     * @param str
     */
    @SuppressWarnings("unchecked")
    public static List<Object> list4(String str) {
        if (null == str)
            return null;
        if ((str.length() > 0 && str.charAt(0) == '[') && str.endsWith("]"))
            return (List<Object>) Json.fromJson(str);
        return (List<Object>) Json.fromJson("[" + str + "]");
    }

    /**
     * <ul>
     * <li>null : 0
     * <li>Map
     * </ul>
     *
     * @param obj
     */
    @Deprecated
    public static int length(Object obj) {
        if (null == obj)
            return 0;
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if (obj instanceof Collection<?>) {
            return ((Collection<?>) obj).size();
        } else if (obj instanceof Map<?, ?>) {
            return ((Map<?, ?>) obj).size();
        }
        try {
            return (Integer) Mirror.me(obj.getClass()).invoke(obj, "length");
        }
        catch (Exception e) {}
        return 1;
    }

    /**
     * <ul>
     * <li>null : 0
     * <li>Map
     * </ul>
     *
     * @param obj
     * @since Nutz 1.r.62
     */
    public static int eleSize(Object obj) {
        if (null == obj)
            return 0;
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        }
        if (obj instanceof Collection<?>) {
            return ((Collection<?>) obj).size();
        }
        // Map
        if (obj instanceof Map<?, ?>) {
            return ((Map<?, ?>) obj).size();
        }
        return 1;
    }

    /**
     *
     * @param obj
     */
    public static Object first(Object obj) {
        if (null == obj)
            return obj;

        if (obj instanceof Collection<?>) {
            Iterator<?> it = ((Collection<?>) obj).iterator();
            return it.hasNext() ? it.next() : null;
        }

        if (obj.getClass().isArray())
            return Array.getLength(obj) > 0 ? Array.get(obj, 0) : null;

        return obj;
    }

    /**
     *
     * @param coll
     */
    public static <T> T first(Collection<T> coll) {
        if (null == coll || coll.isEmpty())
            return null;
        return coll.iterator().next();
    }

    /**
     *
     * @param map
     */
    public static <K, V> Entry<K, V> first(Map<K, V> map) {
        if (null == map || map.isEmpty())
            return null;
        return map.entrySet().iterator().next();
    }

    /**
     */
    public static void Break() throws ExitLoop {
        throw new ExitLoop();
    }

    /**
     */
    public static void Continue() throws ExitLoop {
        throw new ContinueLoop();
    }

    /**
     * <ul>
     * <li>Map
     * </ul>
     *
     * @param obj
     * @param callback
     */
    public static <T> void each(Object obj, Each<T> callback) {
        each(obj, true, callback);
    }

    /**
     * <ul>
     * <li>Map
     * </ul>
     *
     * @param obj
     * @param loopMap
     * @param callback
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> void each(Object obj, boolean loopMap, Each<T> callback) {
        if (null == obj || null == callback)
            return;
        try {
            if (callback instanceof Loop)
                if (!((Loop) callback).begin())
                    return;

            if (obj.getClass().isArray()) {
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++)
                    try {
                        callback.invoke(i, (T) Array.get(obj, i), len);
                    }
                    catch (ContinueLoop e) {}
                    catch (ExitLoop e) {
                        break;
                    }
            } else if (obj instanceof Collection) {
                int len = ((Collection) obj).size();
                int i = 0;
                for (Iterator<T> it = ((Collection) obj).iterator(); it.hasNext();)
                    try {
                        callback.invoke(i++, it.next(), len);
                    }
                    catch (ContinueLoop e) {}
                    catch (ExitLoop e) {
                        break;
                    }
            } else if (loopMap && obj instanceof Map) {
                Map map = (Map) obj;
                int len = map.size();
                int i = 0;
                Class<T> eType = Mirror.getTypeParam(callback.getClass(), 0);
                if (null != eType && eType != Object.class && eType.isAssignableFrom(Entry.class)) {
                    for (Object v : map.entrySet())
                        try {
                            callback.invoke(i++, (T) v, len);
                        }
                        catch (ContinueLoop e) {}
                        catch (ExitLoop e) {
                            break;
                        }

                } else {
                    for (Object v : map.entrySet())
                        try {
                            callback.invoke(i++, (T) ((Entry) v).getValue(), len);
                        }
                        catch (ContinueLoop e) {}
                        catch (ExitLoop e) {
                            break;
                        }
                }
            } else if (obj instanceof Iterator<?>) {
                Iterator<?> it = (Iterator<?>) obj;
                int i = 0;
                while (it.hasNext()) {
                    try {
                        callback.invoke(i++, (T) it.next(), -1);
                    }
                    catch (ContinueLoop e) {}
                    catch (ExitLoop e) {
                        break;
                    }
                }
            } else
                try {
                    callback.invoke(0, (T) obj, 1);
                }
                catch (ContinueLoop e) {}
                catch (ExitLoop e) {}

            if (callback instanceof Loop)
                ((Loop) callback).end();
        }
        catch (LoopException e) {
            throw Lang.wrapThrow(e.getCause());
        }
    }

    /**
     * <p>
     *
     * @param <T>
     * @param array
     * @param index
     */
    public static <T> T get(T[] array, int index) {
        if (null == array)
            return null;
        int i = index < 0 ? array.length + index : index;
        if (i < 0 || i >= array.length)
            return null;
        return array[i];
    }

    /**
     *
     * @param e
     */
    public static String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        StringOutputStream sbo = new StringOutputStream(sb);
        PrintStream ps = new PrintStream(sbo);
        e.printStackTrace(ps);
        ps.flush();
        return sbo.getStringBuilder().toString();
    }

    /**
     * <ul>
     * <li>1 | 0
     * <li>yes | no
     * <li>on | off
     * <li>true | false
     * </ul>
     *
     * @param s
     */
    public static boolean parseBoolean(String s) {
        if (null == s || s.length() == 0)
            return false;
        if (s.length() > 5)
            return true;
        if ("0".equals(s))
            return false;
        s = s.toLowerCase();
        return !"false".equals(s) && !"off".equals(s) && !"no".equals(s);
    }

    /**
     *
     * @throws ParserConfigurationException
     */
    public static DocumentBuilder xmls() throws ParserConfigurationException {
        return Xmls.xmls();
    }

    /**
     *
     * @param millisecond
     */
    public static void quiteSleep(long millisecond) {
        try {
            if (millisecond > 0)
                Thread.sleep(millisecond);
        }
        catch (Throwable e) {}
    }

    /**
     * <ul>
     * </ul>
     *
     * @param s
     */
    public static Number str2number(String s) {
        if (null == s) {
            return 0;
        }
        s = s.toUpperCase();
        if (s.indexOf('.') != -1) {
            char c = s.charAt(s.length() - 1);
            if (c == 'F' || c == 'f') {
                return Float.valueOf(s);
            }
            return Double.valueOf(s);
        }
        if (s.startsWith("0X")) {
            return Integer.valueOf(s.substring(2), 16);
        }
        if (s.charAt(s.length() - 1) == 'L' || s.charAt(s.length() - 1) == 'l') {
            return Long.valueOf(s.substring(0, s.length() - 1));
        }
        Long re = Long.parseLong(s);
        if (Integer.MAX_VALUE >= re && re >= Integer.MIN_VALUE)
            return re.intValue();
        return re;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Map<String, Object>> void obj2map(Object obj,
                                                                T map,
                                                                final Map<Object, Object> memo) {
        if (null == obj || memo.containsKey(obj))
            return;
        memo.put(obj, "");

        // Fix issue #497
        if (obj instanceof Map<?, ?>) {
            map.putAll(__change_map_to_nutmap((Map<String, Object>) obj, memo));
            return;
        }

        Mirror<?> mirror = Mirror.me(obj.getClass());
        Field[] flds = mirror.getFields();
        for (Field fld : flds) {
            Object v = mirror.getValue(obj, fld);
            if (null == v) {
                continue;
            }
            Mirror<?> mr = Mirror.me(v);
            if (mr.isSimple()) {
                map.put(fld.getName(), v);
            }
            else if (memo.containsKey(v)) {
                map.put(fld.getName(), null);
            }
            else if (mr.isColl()) {
                final List<Object> list = new ArrayList<Object>(Lang.length(v));
                Lang.each(v, new Each<Object>() {
                    public void invoke(int index, Object ele, int length) {
                        __join_ele_to_list_as_map(list, ele, memo);
                    }
                });
                map.put(fld.getName(), list);
            }
            // Map
            else if (mr.isMap()) {
                NutMap map2 = __change_map_to_nutmap((Map<String, Object>) v, memo);
                map.put(fld.getName(), map2);
            }
            else {
                T sub;
                try {
                    sub = (T) map.getClass().newInstance();
                }
                catch (Exception e) {
                    throw Lang.wrapThrow(e);
                }
                obj2map(v, sub, memo);
                map.put(fld.getName(), sub);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static NutMap __change_map_to_nutmap(Map<String, Object> map,
                                                 final Map<Object, Object> memo) {
        NutMap re = new NutMap();
        for (Map.Entry<String, Object> en : map.entrySet()) {
            Object v = en.getValue();
            if (null == v)
                continue;
            Mirror<?> mr = Mirror.me(v);
            if (mr.isSimple()) {
                re.put(en.getKey(), v);
            }
            else if (memo.containsKey(v)) {
                continue;
            }
            else if (mr.isColl()) {
                final List<Object> list2 = new ArrayList<Object>(Lang.length(v));
                Lang.each(v, new Each<Object>() {
                    public void invoke(int index, Object ele, int length) {
                        __join_ele_to_list_as_map(list2, ele, memo);
                    }
                });
                re.put(en.getKey(), list2);
            }
            // Map
            else if (mr.isMap()) {
                NutMap map2 = __change_map_to_nutmap((Map<String, Object>) v, memo);
                re.put(en.getKey(), map2);
            }
            else {
                NutMap map2 = obj2nutmap(v);
                re.put(en.getKey(), map2);
            }
        }
        return re;
    }

    @SuppressWarnings("unchecked")
    private static void __join_ele_to_list_as_map(List<Object> list,
                                                  Object o,
                                                  final Map<Object, Object> memo) {
        if (null == o) {
            return;
        }

        if (o instanceof Map<?, ?>) {
            NutMap map2 = __change_map_to_nutmap((Map<String, Object>) o, memo);
            list.add(map2);
            return;
        }

        Mirror<?> mr = Mirror.me(o);
        if (mr.isSimple()) {
            list.add(o);
        }
        else if (memo.containsKey(o)) {
            list.add(null);
        }
        else if (mr.isColl()) {
            final List<Object> list2 = new ArrayList<Object>(Lang.length(o));
            Lang.each(o, new Each<Object>() {
                public void invoke(int index, Object ele, int length) {
                    __join_ele_to_list_as_map(list2, ele, memo);
                }
            });
            list.add(list2);
        }
        // Map
        else if (mr.isMap()) {
            NutMap map2 = __change_map_to_nutmap((Map<String, Object>) o, memo);
            list.add(map2);
        }
        else {
            NutMap map = obj2nutmap(o);
            list.add(map);
        }
    }

    /**
     *
     * @param obj
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj2map(Object obj) {
        return obj2map(obj, HashMap.class);
    }

    /**
     *
     * @param obj
     */
    public static NutMap obj2nutmap(Object obj) {
        return obj2map(obj, NutMap.class);
    }

    /**
     *
     * @param <T>
     * @param obj
     * @param mapType
     */
    public static <T extends Map<String, Object>> T obj2map(Object obj, Class<T> mapType) {
        try {
            T map = mapType.newInstance();
            Lang.obj2map(obj, map, new HashMap<Object, Object>());
            return map;
        }
        catch (Exception e) {
            throw Lang.wrapThrow(e);
        }
    }

    /**
     *
     * @param col
     */
    public static <T> Enumeration<T> enumeration(Collection<T> col) {
        final Iterator<T> it = col.iterator();
        return new Enumeration<T>() {
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            public T nextElement() {
                return it.next();
            }
        };
    }

    /**
     *
     * @param enums
     * @param cols
     */
    public static <T extends Collection<E>, E> T enum2collection(Enumeration<E> enums, T cols) {
        while (enums.hasMoreElements())
            cols.add(enums.nextElement());
        return cols;
    }

    /**
     *
     * @param cs
     */
    public static byte[] toBytes(char[] cs) {
        byte[] bs = new byte[cs.length];
        for (int i = 0; i < cs.length; i++)
            bs[i] = (byte) cs[i];
        return bs;
    }

    /**
     *
     * @param is
     */
    public static byte[] toBytes(int[] is) {
        byte[] bs = new byte[is.length];
        for (int i = 0; i < is.length; i++)
            bs[i] = (byte) is[i];
        return bs;
    }

    /**
     *
     */
    public static boolean isWin() {
        try {
            String os = System.getenv("OS");
            return os != null && os.indexOf("Windows") > -1;
        }
        catch (Throwable e) {
            return false;
        }
    }

    /**
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        }
        catch (Throwable e) {
            return Class.forName(className);
        }
    }

    /**
     *
     */
    public static boolean isJDK6() {
        InputStream is = null;
        try {
            String classFileName = Lang.class.getName().replace('.', '/') + ".class";
            is = ClassTools.getClassLoader().getResourceAsStream(classFileName);
            if (is == null)
                is = ClassTools.getClassLoader().getResourceAsStream("/" + classFileName);
            if (is != null && is.available() > 8) {
                is.skip(7);
                return is.read() > 49;
            }
        }
        catch (Throwable e) {}
        finally {
            Streams.safeClose(is);
        }
        return false;
    }

    /**
     *
     * @param pClass
     */
    public static Object getPrimitiveDefaultValue(Class<?> pClass) {
        if (int.class.equals(pClass))
            return Integer.valueOf(0);
        if (long.class.equals(pClass))
            return Long.valueOf(0);
        if (short.class.equals(pClass))
            return Short.valueOf((short) 0);
        if (float.class.equals(pClass))
            return Float.valueOf(0f);
        if (double.class.equals(pClass))
            return Double.valueOf(0);
        if (byte.class.equals(pClass))
            return Byte.valueOf((byte) 0);
        if (char.class.equals(pClass))
            return Character.valueOf((char) 0);
        if (boolean.class.equals(pClass))
            return Boolean.FALSE;
        return null;
    }

    /**
     *
     * @param me
     * @param field
     */
    public static Type getFieldType(Mirror<?> me, String field) throws NoSuchFieldException {
        return getFieldType(me, me.getField(field));
    }

    /**
     *
     * @param me
     * @param method
     */
    public static Type[] getMethodParamTypes(Mirror<?> me, Method method) {
        Type[] types = method.getGenericParameterTypes();
        List<Type> ts = new ArrayList<Type>();
        for (Type type : types) {
            ts.add(getGenericsType(me, type));
        }
        return ts.toArray(new Type[ts.size()]);
    }

    /**
     *
     * @param me
     * @param field
     */
    public static Type getFieldType(Mirror<?> me, Field field) {
        Type type = field.getGenericType();
        return getGenericsType(me, type);
    }

    /**
     *
     * @param me
     * @param type
     */
    public static Type getGenericsType(Mirror<?> me, Type type) {
        Type[] types = me.getGenericsTypes();
        Type t = type;
        if (type instanceof TypeVariable && types != null && types.length > 0) {
            Type[] tvs = me.getType().getTypeParameters();
            for (int i = 0; i < tvs.length; i++) {
                if (type.equals(tvs[i])) {
                    type = me.getGenericsType(i);
                    break;
                }
            }
        }
        if (!type.equals(t)) {
            return type;
        }
        if (types != null && types.length > 0 && type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;

            if (pt.getActualTypeArguments().length >= 0) {
                NutType nt = new NutType();
                nt.setOwnerType(pt.getOwnerType());
                nt.setRawType(pt.getRawType());
                Type[] tt = new Type[pt.getActualTypeArguments().length];
                for (int i = 0; i < tt.length; i++) {
                    tt[i] = types[i];
                }
                nt.setActualTypeArguments(tt);
                return nt;
            }
        }

        return type;
    }

    /**
     *
     * @param type
     */
    @SuppressWarnings("rawtypes")
    public static Class<?> getTypeClass(Type type) {
        Class<?> clazz = null;
        if (type instanceof Class<?>) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            clazz = (Class<?>) pt.getRawType();
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            Class<?> typeClass = getTypeClass(gat.getGenericComponentType());
            return Array.newInstance(typeClass, 0).getClass();
        } else if (type instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) type;
            Type[] ts = tv.getBounds();
            if (ts != null && ts.length > 0)
                return getTypeClass(ts[0]);
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] t_low = wt.getLowerBounds();
            if (t_low.length > 0)
                return getTypeClass(t_low[0]);
            Type[] t_up = wt.getUpperBounds(); 
            return getTypeClass(t_up[0]);
        }
        return clazz;
    }

    /**
     *
     * @param type
     */
    public static Type[] getGenericsTypes(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return pt.getActualTypeArguments();
        }
        return null;
    }

    /**
     *
     * @param <T>
     * @param name
     * @param type
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> forName(String name, Class<T> type) {
        Class<?> re;
        try {
            re = Lang.loadClass(name);
            return (Class<T>) re;
        }
        catch (ClassNotFoundException e) {
            throw Lang.wrapThrow(e);
        }
    }

    /**
     *
     * @param f
     * @see #digest(String, File)
     */
    public static String md5(File f) {
        return digest("MD5", f);
    }

    /**
     *
     * @param ins
     * @see #digest(String, InputStream)
     */
    public static String md5(InputStream ins) {
        return digest("MD5", ins);
    }

    /**
     *
     * @param cs
     * @see #digest(String, CharSequence)
     */
    public static String md5(CharSequence cs) {
        return digest("MD5", cs);
    }

    /**
     *
     * @param f
     * @see #digest(String, File)
     */
    public static String sha1(File f) {
        return digest("SHA1", f);
    }

    /**
     *
     * @param ins
     * @see #digest(String, InputStream)
     */
    public static String sha1(InputStream ins) {
        return digest("SHA1", ins);
    }

    /**
     *
     * @param cs
     * @see #digest(String, CharSequence)
     */
    public static String sha1(CharSequence cs) {
        return digest("SHA1", cs);
    }

    /**
     *
     * @param f
     * @see #digest(String, File)
     */
    public static String sha256(File f) {
        return digest("SHA-256", f);
    }

    /**
     *
     * @param ins
     * @see #digest(String, InputStream)
     */
    public static String sha256(InputStream ins) {
        return digest("SHA-256", ins);
    }

    /**
     *
     * @param cs
     * @see #digest(String, CharSequence)
     */
    public static String sha256(CharSequence cs) {
        return digest("SHA-256", cs);
    }

    /**
     *
     * @param algorithm
     * @param f
     */
    public static String digest(String algorithm, File f) {
        return digest(algorithm, Streams.fileIn(f));
    }

    /**
     *
     * @param algorithm
     * @param ins
     */
    public static String digest(String algorithm, InputStream ins) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);

            byte[] bs = new byte[HASH_BUFF_SIZE];
            int len = 0;
            while ((len = ins.read(bs)) != -1) {
                md.update(bs, 0, len);
            }

            byte[] hashBytes = md.digest();

            return fixedHexString(hashBytes);
        }
        catch (NoSuchAlgorithmException e) {
            throw Lang.wrapThrow(e);
        }
        catch (FileNotFoundException e) {
            throw Lang.wrapThrow(e);
        }
        catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
        finally {
            Streams.safeClose(ins);
        }
    }

    /**
     *
     * @param algorithm
     * @param cs
     */
    public static String digest(String algorithm, CharSequence cs) {
        return digest(algorithm, Strings.getBytesUTF8(null == cs ? "" : cs), null, 1);
    }

    /**
     *
     * @param algorithm
     * @param bytes
     * @param salt
     * @param iterations
     */
    public static String digest(String algorithm, byte[] bytes, byte[] salt, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);

            if (salt != null) {
                md.update(salt);
            }

            byte[] hashBytes = md.digest(bytes);

            for (int i = 1; i < iterations; i++) {
                md.reset();
                hashBytes = md.digest(hashBytes);
            }

            return fixedHexString(hashBytes);
        }
        catch (NoSuchAlgorithmException e) {
            throw Lang.wrapThrow(e);
        }
    }

    /** 当前运行的 Java 虚拟机是否是在安卓环境 */
    public static final boolean isAndroid;

    static {
        boolean flag = false;
        try {
            Class.forName("android.Manifest");
            flag = true;
        }
        catch (Throwable e) {}
        isAndroid = flag;
    }

    /**
     *
     * @param arrays
     */
    public static <T> void reverse(T[] arrays) {
        int size = arrays.length;
        for (int i = 0; i < size; i++) {
            int ih = i;
            int it = size - 1 - i;
            if (ih == it || ih > it) {
                break;
            }
            T ah = arrays[ih];
            T swap = arrays[it];
            arrays[ih] = swap;
            arrays[it] = ah;
        }
    }

    @Deprecated
    public static String simpleMetodDesc(Method method) {
        return simpleMethodDesc(method);
    }

    public static String simpleMethodDesc(Method method) {
        return String.format("%s.%s(...)",
                             method.getDeclaringClass().getSimpleName(),
                             method.getName());
    }

    public static String fixedHexString(byte[] hashBytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    /**
     *
     * @param ms
     */
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            throw Lang.wrapThrow(e);
        }
    }

    /**
     *
     * @param lock
     * @param ms
     */
    public static void wait(Object lock, long ms) {
        if (null != lock)
            synchronized (lock) {
                try {
                    lock.wait(ms);
                }
                catch (InterruptedException e) {
                    throw Lang.wrapThrow(e);
                }
            }
    }

    /**
     *
     * @param lock
     */
    public static void notifyAll(Object lock) {
        if (null != lock)
            synchronized (lock) {
                lock.notifyAll();
            }
    }

    public static void runInAnThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    /**
     *
     * @param source
     * @param prefix
     * @param include
     * @param exclude
     * @param keyMap
     */
    public static Map<String, Object> filter(Map<String, Object> source,
                                             String prefix,
                                             String include,
                                             String exclude,
                                             Map<String, String> keyMap) {
        LinkedHashMap<String, Object> dst = new LinkedHashMap<String, Object>();
        if (source == null || source.isEmpty())
            return dst;

        Pattern includePattern = include == null ? null : Pattern.compile(include);
        Pattern excludePattern = exclude == null ? null : Pattern.compile(exclude);

        for (Entry<String, Object> en : source.entrySet()) {
            String key = en.getKey();
            if (prefix != null) {
                if (key.startsWith(prefix))
                    key = key.substring(prefix.length());
                else
                    continue;
            }
            if (includePattern != null && !includePattern.matcher(key).find())
                continue;
            if (excludePattern != null && excludePattern.matcher(key).find())
                continue;
            if (keyMap != null && keyMap.containsKey(key))
                dst.put(keyMap.get(key), en.getValue());
            else
                dst.put(key, en.getValue());
        }
        return dst;
    }

    /**
     *
     * @param request
     */
    public static String getIP(HttpServletRequest request) {
        if (request == null)
            return "";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } else if (ip.length() > 15) {
            String[] ips = ip.split(",");
            for (int index = 0; index < ips.length; index++) {
                String strIp = ips[index];
                if (!("unknown".equalsIgnoreCase(strIp))) {
                    ip = strIp;
                    break;
                }
            }
        }
        if (Strings.isBlank(ip))
            return "";
        if (isIPv4Address(ip) || isIPv6Address(ip)) {
            return ip;
        }
        return "";
    }

    /**
     */
    public static String runRootPath() {
        String cp = Lang.class.getClassLoader().getResource("").toExternalForm();
        if (cp.startsWith("file:")) {
            cp = cp.substring("file:".length());
        }
        return cp;
    }

    public static <T> T copyProperties(Object origin, T target) {
        return copyProperties(origin, target, null, null, false, true);
    }

    public static <T> T copyProperties(Object origin,
                                       T target,
                                       String active,
                                       String lock,
                                       boolean ignoreNull,
                                       boolean ignoreStatic) {
        if (origin == null)
            throw new IllegalArgumentException("origin is null");
        if (target == null)
            throw new IllegalArgumentException("target is null");
        Pattern at = active == null ? null : Pattern.compile(active);
        Pattern lo = lock == null ? null : Pattern.compile(lock);
        Mirror<Object> originMirror = Mirror.me(origin);
        Mirror<T> targetMirror = Mirror.me(target);
        Field[] fields = targetMirror.getFields();
        for (Field field : originMirror.getFields()) {
            String name = field.getName();
            if (at != null && !at.matcher(name).find())
                continue;
            if (lo != null && lo.matcher(name).find())
                continue;
            if (ignoreStatic && Modifier.isStatic(field.getModifiers()))
                continue;
            Object val = originMirror.getValue(origin, field);
            if (ignoreNull && val == null)
                continue;
            for (Field _field : fields) {
                if (_field.getName().equals(field.getName())) {
                    targetMirror.setValue(target, _field, val);
                }
            }
        }
        return target;
    }

    public static StringBuilder execOutput(String cmd) throws IOException {
        return execOutput(Strings.splitIgnoreBlank(cmd, " "), Encoding.CHARSET_UTF8);
    }

    public static StringBuilder execOutput(String cmd, Charset charset) throws IOException {
        return execOutput(Strings.splitIgnoreBlank(cmd, " "), charset);
    }

    public static StringBuilder execOutput(String cmd[]) throws IOException {
        return execOutput(cmd, Encoding.CHARSET_UTF8);
    }

    public static StringBuilder execOutput(String[] cmd, Charset charset) throws IOException {
        Process p = Runtime.getRuntime().exec(cmd);
        p.getOutputStream().close();
        InputStreamReader r = new InputStreamReader(p.getInputStream(), charset);
        StringBuilder sb = new StringBuilder();
        Streams.readAndClose(r, sb);
        return sb;
    }

    public static void exec(String cmd, StringBuilder out, StringBuilder err) throws IOException {
        exec(Strings.splitIgnoreBlank(cmd, " "), Encoding.CHARSET_UTF8, out, err);
    }

    public static void exec(String[] cmd, StringBuilder out, StringBuilder err) throws IOException {
        exec(cmd, Encoding.CHARSET_UTF8, out, err);
    }

    public static void exec(String[] cmd, Charset charset, StringBuilder out, StringBuilder err)
            throws IOException {
        Process p = Runtime.getRuntime().exec(cmd);
        p.getOutputStream().close();
        InputStreamReader sOut = new InputStreamReader(p.getInputStream(), charset);
        Streams.readAndClose(sOut, out);

        InputStreamReader sErr = new InputStreamReader(p.getErrorStream(), charset);
        Streams.readAndClose(sErr, err);
    }

    public static Class<?> loadClassQuite(String className) {
        try {
            return loadClass(className);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static byte[] toBytes(Object obj) {
        try {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bao);
            oos.writeObject(obj);
            return bao.toByteArray();
        }
        catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromBytes(byte[] buf, Class<T> klass) {
        try {
            return (T) new ObjectInputStream(new ByteArrayInputStream(buf)).readObject();
        }
        catch (ClassNotFoundException e) {
            return null;
        }
        catch (IOException e) {
            return null;
        }
    }
}
