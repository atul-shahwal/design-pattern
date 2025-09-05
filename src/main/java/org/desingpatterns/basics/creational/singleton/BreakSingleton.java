package org.desingpatterns.basics.creational.singleton;


/*
    * 1. Reflection API to break singleton pattern
    *   solution of reflection api to prevent reflection api there are two ways
    *   1-> if object is there ==> throw exception from inside the constructor.
    *   2-> use enum
    * 2. Deserialization to break singleton design pattern
    *   Solution --> implement readResolve method
    * 3. Cloning will also break singleton design pattern.
    *   solution insted of returning super.clone() return static object
 */
public class BreakSingleton {
    public static void main(String[] args) throws Exception {
//        StudentLazy obj1 = StudentLazy.getStudent();
//        System.out.println(obj1.hashCode());
//        usning reflection api
//        Constructor<StudentLazy> constructor = StudentLazy.class.getDeclaredConstructor();
//        StudentLazy obj2 = constructor.newInstance();
//        System.out.println(obj2.hashCode());
//        you can see we got error because constructor was private
//        we have to chage accessibility
//        Constructor<StudentLazy> constructor = StudentLazy.class.getDeclaredConstructor();
//        constructor.setAccessible(true); // we have changed the accessiblity of the constructor
//        StudentLazy obj2 = constructor.newInstance();
//        System.out.println(obj2.hashCode());

        //for enum class logic only below code will work
//        StudentLazy obj1 = StudentLazy.INSTANCE;
//        System.out.println(obj1.hashCode());
//        obj1.getTest();
//        Constructor<StudentLazy> constructor = StudentLazy.class.getDeclaredConstructor();
//        constructor.setAccessible(true);
//        StudentLazy obj2 = constructor.newInstance();
//        System.out.println(obj2.hashCode());

        //serialize and desearilize
//        StudentLazy obj1 = StudentLazy.getStudent();
//        System.out.println(obj1.hashCode());
//        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("abc.ob"));
//        oos.writeObject(obj1);
//        System.out.println("Object Serialization done...");
//        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("abc.ob"));
//        StudentLazy obj2 = (StudentLazy) ois.readObject();
//        System.out.println(obj2.hashCode());

        StudentLazy obj1 = StudentLazy.getStudent();
        System.out.println(obj1.hashCode());
        StudentLazy obj2 = (StudentLazy) obj1.clone();
        System.out.println(obj2.hashCode());
    }
}
