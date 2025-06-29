package com.erik.git_bro.TestUtils;
/*
 * To generate keys, run these commands:
 *      openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048
 *      openssl rsa -in private_key.pem -outform PEM -pubout -out public_key.pem 
 * 
 */
public class DummyKeys {

        public static final String VALID_PUBLIC_KEY = """
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqcwDgNZUKQDN0CxEdQmc
gHg/gChWNt4G3x4TJ2ECvbwkRw8cD/LL2v2UsFyBUsqEsWSutJxoVrEa6aMw+3d0
bXmQghZ2k3935HzlIaiwmWRPOraam+7UsAxsIv4RZRULDJa1LatO5BW+k5gZCl5f
uY/3+ub57hllX8dwySOGIagbBFwi9IVwC/say2CMaNI8ujasyN1gGWP/vqSUpl9Q
zg7hrDoeVOdc1PJV1BspdQ2ENY/MG1vYsXmRDIBU2iMSoScjhoGDOFnfPQWJdNgI
LjjKuTT/jdQd/HDGYLEH4xyTpFLx7F5bTO2kOSclasLIUeI+5YGwzXZ/DKl98n6q
swIDAQAB
-----END PUBLIC KEY-----

                        """;
        public static final String VALID_PRIVATE_PEM = """
-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCpzAOA1lQpAM3Q
LER1CZyAeD+AKFY23gbfHhMnYQK9vCRHDxwP8sva/ZSwXIFSyoSxZK60nGhWsRrp
ozD7d3RteZCCFnaTf3fkfOUhqLCZZE86tpqb7tSwDGwi/hFlFQsMlrUtq07kFb6T
mBkKXl+5j/f65vnuGWVfx3DJI4YhqBsEXCL0hXAL+xrLYIxo0jy6NqzI3WAZY/++
pJSmX1DODuGsOh5U51zU8lXUGyl1DYQ1j8wbW9ixeZEMgFTaIxKhJyOGgYM4Wd89
BYl02AguOMq5NP+N1B38cMZgsQfjHJOkUvHsXltM7aQ5JyVqwshR4j7lgbDNdn8M
qX3yfqqzAgMBAAECggEAU8sgd3ZN9iY1a2RNLEzf9O5iuAYv9cfAMF+RKD/RsHym
koK8YIp7W8kElbww/gB/V2POa8c8HnwsQdyNEawzwsrZK7mWLtga/KE+Gkw7Spec
Ged/iTBlubOHsgUb5HxsBXxiUPVXa2vnrsLCqMFGEDciM6nE8WGLC6dK25gyW2zi
kMH+g67lfKBCmO7V/xFoG5RjChU2A9oGp0VzwBfeDDdYmqX7372uB8UzPAmAj3+O
Pq46FYCt9L9fgwBSbgeayOD7JCOriecAy5nKXjTRBWCHxDtyIZPoW9AYBmZm2VKw
jVkgYNkVTDbi3VPvRg0PaFL6OlnSsz7NSqV1rzJUgQKBgQDQzhw6sXPC7pPZxJyq
VIssCjgaTxGQ4CrH+Dw7ak1hvKT+HziA91CLKA1hjf0Go7PMUfYP3jutIjG1qzDt
wkgXDNrDh4SBv7QpNDBTvuIR5CBtV4muiRBJg0GTQ6pqSu4F/qA+QHVjKIADY1b7
mbRk+JKcYk/sAQ63YEorBTVnkwKBgQDQLM9fL4G/xG+qk6nx5RtEkrQZYdeWOWW2
2r/WXW24QtebD8Od/DOX5bON2sdtCl9C2ZuIsiAILq+4iJZ0tHQXhE+izxwJY9tv
cK2mHAtW+4O8cIJHt8N7cXCE0/60xd+K8Jyq4rh2ahUT+JI+ig1pLfKLmEPKrdcM
6/FkZh9kYQKBgEQlkIcDVUGgdbaT+YGJuCY2a5sChTwgPTYoaRgHGJw2Yi7h56IH
lUzlVICrQ5JAKTUc5s7E+/sEdu7QoVZnmvmS427/MpraAyWTd9ZNLSEjsAWflTrW
Vu93HBbA9cRdEhP4xzmp9eXX2PfCGyhgQMTXb5oEaAshSTlF/s7Nk2FrAoGBAIlJ
Q+juF480KMBBaY4RHY53gTZMBDUZ3S5dzK2+J6Gg0zd7ifIiZJQD9AnVAjey42fe
wme/sMg0ocX8rdyBNR5dL3ebRk4bAJfZo2ssi33tmM8pE4HgepZqFV5DwHHPHMEw
NOGtayEMjKIDB88elAGNP6s3211Kj8wBvcwMXR+BAoGBAMXxRWL/5vIwcW5SsAdK
ZblMoC721MdbInalAzing1DdJAMhFpsU4gpYkuRxqGFhE1d7hddDSxRmjGtPIY7E
rkWd22MpRHfkAdPjfgBmY/ags3d9k3H4cWNcci+wyKdx5ujSbQLPGJ/jK9FviM5e
js4XFR3rQJID4lEiD92l0mVX
-----END PRIVATE KEY-----

                                """;

}
