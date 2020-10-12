/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_brewchain_evfs_jni_Merklezksnark */

#ifndef _Included_org_brewchain_evfs_jni_Merklezksnark
#define _Included_org_brewchain_evfs_jni_Merklezksnark
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    createMT
 * Signature: ([II)J
 */
JNIEXPORT jlong JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_createMT
  (JNIEnv *, jobject, jintArray, jint);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    restoreMT
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_restoreMT
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    destoryMT
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_destoryMT
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_init
  (JNIEnv *, jobject);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    pushNode
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_pushNode
  (JNIEnv *, jobject, jlong, jbyteArray);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    getPK
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_getPK
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    getVK
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_getVK
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    prove
 * Signature: (JIZ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_prove
  (JNIEnv *, jobject, jlong, jint, jboolean);

/*
 * Class:     org_brewchain_evfs_jni_Merklezksnark
 * Method:    verify
 * Signature: ([B[B[B)Z
 */
JNIEXPORT jboolean JNICALL Java_org_brewchain_evfs_jni_Merklezksnark_verify
  (JNIEnv *, jobject, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
