package seckill.demo.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;


public class MD5Util {

    public static String md5(String src){
        return DigestUtils.md5Hex(src);
    }

    private static final String salt = "1a2b3c4d";

    /**
     * 对客户端输入的密码加盐（第一次加盐），得到的MD5值为表单中传输的值
     * <p>
     * 在密码的传输和存储中，总共经历了两次MD5和两次salt
     * <p>
     * 第一次：客户输入到表单中的密码会经过一次MD5和加盐，即；pwd_md_1st = MD5（用户明文密码）+ salt_1st。
     * 其中，pwd_md_1st 是客户端真正接收到的密码。salt_1st在客户端和服务端都是一样的
     * <p>
     * 第二次：对客户端传递到服务器的 pwd_md_1st 再一次MD5和加盐，即；pwd_md_2nd = MD5（MD5和加盐）+ salt_2nd。
     * 其中，salt_2nd是存储在服务器端的，每个用户都有自己的salt_2nd，所以在使用salt_2nd时需要从数据库中查出
     * <p>
     * 最终存储在数据库中的用户密码实际为 pwd_md_2nd。
     *
     * @param inputPass 用户输入的密码
     * @return Calculates the MD5 digest and returns the value as a 32 character hex string.
     */
    public static String inputPassFromPass(String inputPass){
        // 加盐规则（自定义）
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass +salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    /**
     * 对表单中的密码md5加盐，加盐后的md5为存储在数据库中的密码md5
     *
     * @param fromPass 表单中填充的明文密码
     * @param salt       这里的salt是在数据库查出来的，而并非第一次加盐的盐值
     * @return
     */
    public static String fromPassToDBPass(String fromPass,String salt){
        String str = ""+salt.charAt(0) + salt.charAt(2) + fromPass +salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    /**
     *  两次MD5：储存在数据库中的密码的md5 = MD5(MD5(input_password) + salt) + db_salt
     * @param input
     * @param saltDB
     * @return
     */
    public static String inputPassToDBPass(String input,String saltDB){
        String inputPassFromPass = inputPassFromPass(input);
        String fromPassToDBPass = fromPassToDBPass(inputPassFromPass,saltDB);
        return fromPassToDBPass;
    }

    @Test
    public void TestMD5(){
        System.out.println(inputPassFromPass("123456"));
        System.out.println(fromPassToDBPass(inputPassFromPass("123456"),"1a2b3c4d"));
        System.out.println(inputPassToDBPass("123456","1a2b3c4d"));
    }
    //d3b1294a61a07da9b49b6e22b2cbd7f9


}
