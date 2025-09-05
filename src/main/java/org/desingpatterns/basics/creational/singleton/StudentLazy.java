package org.desingpatterns.basics.creational.singleton;

/*
    * Singleton means only single object should be created.
    * when we want to create only one object of a class and want to reuse it again and again the same object.
    * eg- work related to database means we want to create connection only once.
    * points to remember
    * 1.stop constructor call making it private
    * 2. object creation through method(called Factory method)
    * Types of initialization
    * 1.lazy
    * 2.eager
    * problem with lazy we in our lazy way we need to take in account the threads
    * because threads executes parallels suppose two Thread(t1,t2)
    * t2 and t2 both tried to execute same method then there will be two object created
    * but in singleton we can only have one object so we need to  take in account using
    * synchronized keyword means at any instant only one thread can execute it other have to wait
    * until its get executed
 */
// public class StudentLazy {
//    //create a variable
//    private static StudentLazy studentLazy;
//    // constructor call we have to stop by making it private.
//    private StudentLazy(){
//
//    }
//    // create method to create object
//    // 1.lazy way of creating object
//    // two ways to use synchronised word
//    // 1. method with synchronised--> entire method has become thread safe which is not good we only have to make
//    // object creation thread safe.
////    public synchronized static StudentLazy getStudent(){
////        if(studentLazy == null){
////            studentLazy = new StudentLazy();
////        }
////        return studentLazy;
////    }
//    public static StudentLazy getStudent(){
//        if(studentLazy == null){
//            synchronized (StudentLazy.class){
//                if(studentLazy == null){
//                    studentLazy = new StudentLazy();
//                }
//            }
//        }
//        return studentLazy;
//    }
//}


//saving singleton design pattern.
//1.
//public class StudentLazy {
//    private static StudentLazy studentLazy;
//    // if object is not null trow exception from inside the constructor.
//    private StudentLazy(){
//        if(studentLazy != null){
//            throw new RuntimeException("You are trying to break design pattern");
//        }
//    }
//    public static StudentLazy getStudent(){
//        if(studentLazy == null){
//            synchronized (StudentLazy.class){
//                if(studentLazy == null){
//                    studentLazy = new StudentLazy();
//                }
//            }
//        }
//        return studentLazy;
//    }
//}

//2. make class enum
//public enum StudentLazy {
//    INSTANCE;
//
//    public void getTest(){
//        System.out.println("this is test methos");
//    }
//}

// Deserialization purpose

//public class StudentLazy implements Serializable {
//    private static StudentLazy studentLazy;
//    private StudentLazy(){
//
//    }
//    public static StudentLazy getStudent(){
//        if(studentLazy == null){
//            synchronized (StudentLazy.class){
//                if(studentLazy == null){
//                    studentLazy = new StudentLazy();
//                }
//            }
//        }
//        return studentLazy;
//    }
//    // solution to serialization
//    public Object readResolve(){
//        return studentLazy;
//    }
//}


//for cloning example
public class StudentLazy implements Cloneable{
//    @Override
//    protected Object clone() throws CloneNotSupportedException {
//        return super.clone();
//    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return studentLazy;
    }
    private static StudentLazy studentLazy;

    private StudentLazy(){
    }
    public static StudentLazy getStudent(){
        if(studentLazy == null){
            synchronized (StudentLazy.class){
                if(studentLazy == null){
                    studentLazy = new StudentLazy();
                }
            }
        }
        return studentLazy;
    }
}