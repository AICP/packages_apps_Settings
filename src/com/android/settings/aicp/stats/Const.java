package com.android.settings.aicp.stats;

public class Const {

	public static final String TAG = "ROMStats";
	
	public static final String ANONYMOUS_OPT_IN = "pref_anonymous_opt_in";
	public static final String ANONYMOUS_OPT_OUT_PERSIST = "pref_anonymous_opt_out_persist";
	public static final String ANONYMOUS_FIRST_BOOT = "pref_anonymous_first_boot";
	public static final String ANONYMOUS_LAST_CHECKED = "pref_anonymous_checked_in";
	public static final String ANONYMOUS_LAST_REPORT_VERSION = "pref_anonymous_last_rep_version";
	public static final String ANONYMOUS_NEXT_ALARM = "pref_anonymous_next_alarm";
	
	public static final int ROMSTATS_REPORTING_MODE_NEW = 0; // new CM10.1: no user prompt, default TRUE, first time after tframe
	public static final int ROMSTATS_REPORTING_MODE_OLD = 1; // old CM10  : user prompt, default FALSE, first time immediately
	
	public static final String CERT_PUB_KEY = "3082030d308201f5a00302010202042b7cebb9300d06092a864886f70d01010b05003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3133303331383135323732375a170d3433303331313135323732375a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730820122300d06092a864886f70d01010105000382010f003082010a0282010100890a1dec24c15e70e200eef3f7d876b9848e1746077420c243a72d9b987168896ed27c9ea608d7884b3e572b56e4355c7ab8c6f967d179ceda97609d5dd693f438f68514475c4ec0e018bad334069dcf5391fa2a1a85ac5bab2a9af061ef1762d808542884fc6472a108938d867eef19cbc000993d46cf0ef9516c50290e4e844102ddd73004503447960ff934942bbf49aa33338cc927da611cb0516d3c2cd021cff7acd96fa700e44b31558e9ea77a0bf298d4df9b6ac95e531b6a2a85b35af3581097fed5794d44bff29ff190b94aaed448a3c545aac20ad31ff88c59f422c2f208a2b3b18a3482f26cd563ef5c91680992d3b2f9d178659f41f98910657f0203010001a321301f301d0603551d0e04160414f22c478bcbcf005576c9b96ed36eb4946babeb73300d06092a864886f70d01010b050003820101004c247e16396d674b72862024ce6c9682ebc78b4e48d26e93cbebf1c5f9d4642245c9710d37d7eee2c85c73fc958a9394de4142952ef8123164c6c9182e3b27cbb26d8c8d9af3a7ba0b28fa7fafa12c6270d63b03813165b5d3ce587436b523db6ac168a7a79db85ccb25dcb4f0840a9cdaabff6dd63523f26b1060f6ee1c18fa746e3120a1c17fae302bfdadc7638e713e566684e715c9c2934b55342347fdbe991c4a0f8514b2a7e7853aaa3d3fc7622d6efd259993b34ed027ad1bdd37b97058c177fef914112e2277f05114857f3481cf947dd563d58cd29b66abea2c46255ce2a9e9a4407fb3629019490c4f4d5291fab4d423b75b5c283cbadf7de2b68f";

}
