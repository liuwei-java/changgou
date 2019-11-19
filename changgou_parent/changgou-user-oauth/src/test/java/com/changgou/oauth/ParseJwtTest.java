package com.changgou.oauth;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

public class ParseJwtTest {

    @Test
    public void parseJwt(){
        //基于公钥去解析jwt
        String jwt ="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJhcHAiXSwibmFtZSI6bnVsbCwiaWQiOm51bGwsImV4cCI6MTU3MzM0NjQ0NywiYXV0aG9yaXRpZXMiOlsiYWNjb3VudGFudCIsInVzZXIiLCJzYWxlc21hbiJdLCJqdGkiOiJhNWRjZjMwNi0yZjNhLTQxMzgtOTQ2Zi0wZDQ0ZjY5ZmE4OTEiLCJjbGllbnRfaWQiOiJjaGFuZ2dvdSIsInVzZXJuYW1lIjoiaGVpbWEifQ.gdv4UaUpHHJWc0Rh3PAWap-ALvddL9_qm34MoNBkdd-Au15nIgt2ixvqSVeyHQWYTnZJ-ItFcD7Jpo87I0Kw7iDc1MZZuN9CJtiaRYnTd5utReijSab0tgAMnQKNBgm6bv5h4TgmxE-6uRC1Eiw5DKZdT5hNceyaQglU6hPw_c2hA5ABVAJV2gBVRh5mRKZUTSZGRKfJW99lvhEveIzyfUTZluDt_U_jaMPX_85IPGZ11MOMpZS6AQx8QUxBATijfkUSiPilJKoVYtA7Gt-0tdP7mltdl9j887qaqf2fz_v4lEjQ5E9tJWfwcAqMwIEyo8Y3YKA_tQzUvS6Trlnzgw";
        String publicKey ="-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvFsEiaLvij9C1Mz+oyAmt47whAaRkRu/8kePM+X8760UGU0RMwGti6Z9y3LQ0RvK6I0brXmbGB/RsN38PVnhcP8ZfxGUH26kX0RK+tlrxcrG+HkPYOH4XPAL8Q1lu1n9x3tLcIPxq8ZZtuIyKYEmoLKyMsvTviG5flTpDprT25unWgE4md1kthRWXOnfWHATVY7Y/r4obiOL1mS5bEa/iNKotQNnvIAKtjBM4RlIDWMa6dmz+lHtLtqDD2LF1qwoiSIHI75LQZ/CNYaHCfZSxtOydpNKq8eb1/PGiLNolD4La2zf0/1dlcr5mkesV570NxRmU1tFm8Zd3MZlZmyv9QIDAQAB-----END PUBLIC KEY-----";

        Jwt token = JwtHelper.decodeAndVerify(jwt, new RsaVerifier(publicKey));

        String claims = token.getClaims();
        System.out.println(claims);
    }
}
