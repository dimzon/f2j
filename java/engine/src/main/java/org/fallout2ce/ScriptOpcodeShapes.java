package org.fallout2ce;

/**
 * The stack effect of every opcode the script VM knows.
 *
 * <p>For each dispatch slot: the values the opcode reads off the value
 * stack, in the order it reads them (top of the stack first), and the
 * kind of value it leaves behind - none, an int, a string, a pointer or a
 * value that may be either.
 *
 * <p>Generated from the C handlers; see the commit that added this file.
 */
final class ScriptOpcodeShapes {

    /** The kinds a value on the stack can have. */
    static final int INT = 0;
    static final int STRING = 1;
    static final int POINTER = 2;
    static final int VALUE = 3;

    /** How many values each slot reads, and what it leaves behind. */
    static final byte[] POPS = new byte[768];
    static final byte[] POP_KIND = new byte[768];
    static final byte[] PUSH_KIND = new byte[768];

    /** The number of values each slot reads. */
    static final int[] POP_COUNT = new int[768];

    static {
        POP_COUNT[0] = 0;
        PUSH_KIND[0] = -1;
    }
    static {
        POP_COUNT[1] = 0;
        PUSH_KIND[1] = 3;
    }
    static {
        POP_COUNT[2] = 0;
        PUSH_KIND[2] = -1;
    }
    static {
        POP_COUNT[3] = 0;
        PUSH_KIND[3] = -1;
    }
    static {
        POP_COUNT[4] = 1;
        POP_KIND[4 + 0] = 0;
        PUSH_KIND[4] = -1;
    }
    static {
        POP_COUNT[5] = 1;
        POP_KIND[5 + 0] = 0;
        PUSH_KIND[5] = -1;
    }
    static {
        POP_COUNT[6] = 2;
        POP_KIND[6 + 0] = 0;
        POP_KIND[6 + 1] = 0;
        PUSH_KIND[6] = -1;
    }
    static {
        POP_COUNT[7] = 2;
        POP_KIND[7 + 0] = 0;
        POP_KIND[7 + 1] = 0;
        PUSH_KIND[7] = -1;
    }
    static {
        POP_COUNT[8] = 0;
        PUSH_KIND[8] = -1;
    }
    static {
        POP_COUNT[9] = 0;
        PUSH_KIND[9] = -1;
    }
    static {
        POP_COUNT[10] = 0;
        PUSH_KIND[10] = -1;
    }
    static {
        POP_COUNT[11] = 0;
        PUSH_KIND[11] = -1;
    }
    static {
        POP_COUNT[12] = 0;
        PUSH_KIND[12] = 3;
    }
    static {
        POP_COUNT[13] = 1;
        POP_KIND[13 + 0] = 3;
        PUSH_KIND[13] = -1;
    }
    static {
        POP_COUNT[14] = 0;
        PUSH_KIND[14] = -1;
    }
    static {
        POP_COUNT[15] = 0;
        PUSH_KIND[15] = -1;
    }
    static {
        POP_COUNT[16] = 0;
        PUSH_KIND[16] = -1;
    }
    static {
        POP_COUNT[17] = 0;
        PUSH_KIND[17] = -1;
    }
    static {
        POP_COUNT[18] = 1;
        POP_KIND[18 + 0] = 0;
        PUSH_KIND[18] = 3;
    }
    static {
        POP_COUNT[19] = 2;
        POP_KIND[19 + 0] = 0;
        POP_KIND[19 + 1] = 3;
        PUSH_KIND[19] = -1;
    }
    static {
        POP_COUNT[20] = 1;
        POP_KIND[20 + 0] = 3;
        PUSH_KIND[20] = 3;
    }
    static {
        POP_COUNT[21] = 2;
        POP_KIND[21 + 0] = 3;
        POP_KIND[21 + 1] = 3;
        PUSH_KIND[21] = -1;
    }
    static {
        POP_COUNT[22] = 1;
        POP_KIND[22 + 0] = 3;
        PUSH_KIND[22] = -1;
    }
    static {
        POP_COUNT[23] = 2;
        POP_KIND[23 + 0] = 0;
        POP_KIND[23 + 1] = 0;
        PUSH_KIND[23] = -1;
    }
    static {
        POP_COUNT[24] = 2;
        POP_KIND[24 + 0] = 3;
        POP_KIND[24 + 1] = 3;
        PUSH_KIND[24] = 3;
    }
    static {
        POP_COUNT[25] = 0;
        PUSH_KIND[25] = -1;
    }
    static {
        POP_COUNT[26] = 1;
        POP_KIND[26 + 0] = 3;
        PUSH_KIND[26] = -1;
    }
    static {
        POP_COUNT[27] = 1;
        POP_KIND[27 + 0] = 3;
        PUSH_KIND[27] = 3;
    }
    static {
        POP_COUNT[28] = 0;
        PUSH_KIND[28] = -1;
    }
    static {
        POP_COUNT[29] = 0;
        PUSH_KIND[29] = -1;
    }
    static {
        POP_COUNT[30] = 0;
        PUSH_KIND[30] = -1;
    }
    static {
        POP_COUNT[31] = 3;
        POP_KIND[31 + 0] = 0;
        POP_KIND[31 + 1] = 2;
        POP_KIND[31 + 2] = 0;
        PUSH_KIND[31] = -1;
    }
    static {
        POP_COUNT[32] = 0;
        PUSH_KIND[32] = -1;
    }
    static {
        POP_COUNT[33] = 0;
        PUSH_KIND[33] = -1;
    }
    static {
        POP_COUNT[34] = 0;
        PUSH_KIND[34] = -1;
    }
    static {
        POP_COUNT[35] = 0;
        PUSH_KIND[35] = -1;
    }
    static {
        POP_COUNT[36] = 1;
        POP_KIND[36 + 0] = 3;
        PUSH_KIND[36] = 3;
    }
    static {
        POP_COUNT[37] = 1;
        POP_KIND[37 + 0] = 3;
        PUSH_KIND[37] = 3;
    }
    static {
        POP_COUNT[38] = 1;
        POP_KIND[38 + 0] = 3;
        PUSH_KIND[38] = 3;
    }
    static {
        POP_COUNT[39] = 2;
        POP_KIND[39 + 0] = 0;
        POP_KIND[39 + 1] = 0;
        PUSH_KIND[39] = -1;
    }
    static {
        POP_COUNT[40] = 1;
        POP_KIND[40 + 0] = 1;
        PUSH_KIND[40] = 0;
    }
    static {
        POP_COUNT[41] = 0;
        PUSH_KIND[41] = -1;
    }
    static {
        POP_COUNT[42] = 0;
        PUSH_KIND[42] = -1;
    }
    static {
        POP_COUNT[43] = 1;
        POP_KIND[43 + 0] = 0;
        PUSH_KIND[43] = -1;
    }
    static {
        POP_COUNT[44] = 0;
        PUSH_KIND[44] = -1;
    }
    static {
        POP_COUNT[45] = 1;
        POP_KIND[45 + 0] = 0;
        PUSH_KIND[45] = 0;
    }
    static {
        POP_COUNT[46] = 1;
        POP_KIND[46 + 0] = 0;
        PUSH_KIND[46] = -1;
    }
    static {
        POP_COUNT[47] = 1;
        POP_KIND[47 + 0] = 3;
        PUSH_KIND[47] = -1;
    }
    static {
        POP_COUNT[48] = 1;
        POP_KIND[48 + 0] = 3;
        PUSH_KIND[48] = -1;
    }
    static {
        POP_COUNT[49] = 2;
        POP_KIND[49 + 0] = 0;
        POP_KIND[49 + 1] = 3;
        PUSH_KIND[49] = -1;
    }
    static {
        POP_COUNT[50] = 1;
        POP_KIND[50 + 0] = 0;
        PUSH_KIND[50] = 3;
    }
    static {
        POP_COUNT[51] = 2;
        POP_KIND[51 + 0] = 3;
        POP_KIND[51 + 1] = 3;
        PUSH_KIND[51] = 0;
    }
    static {
        POP_COUNT[52] = 2;
        POP_KIND[52 + 0] = 3;
        POP_KIND[52 + 1] = 3;
        PUSH_KIND[52] = 0;
    }
    static {
        POP_COUNT[53] = 2;
        POP_KIND[53 + 0] = 3;
        POP_KIND[53 + 1] = 3;
        PUSH_KIND[53] = 0;
    }
    static {
        POP_COUNT[54] = 2;
        POP_KIND[54 + 0] = 3;
        POP_KIND[54 + 1] = 3;
        PUSH_KIND[54] = 0;
    }
    static {
        POP_COUNT[55] = 2;
        POP_KIND[55 + 0] = 3;
        POP_KIND[55 + 1] = 3;
        PUSH_KIND[55] = 0;
    }
    static {
        POP_COUNT[56] = 2;
        POP_KIND[56 + 0] = 3;
        POP_KIND[56 + 1] = 3;
        PUSH_KIND[56] = 0;
    }
    static {
        POP_COUNT[57] = 2;
        POP_KIND[57 + 0] = 3;
        POP_KIND[57 + 1] = 3;
        PUSH_KIND[57] = 3;
    }
    static {
        POP_COUNT[58] = 2;
        POP_KIND[58 + 0] = 3;
        POP_KIND[58 + 1] = 3;
        PUSH_KIND[58] = 0;
    }
    static {
        POP_COUNT[59] = 2;
        POP_KIND[59 + 0] = 3;
        POP_KIND[59 + 1] = 3;
        PUSH_KIND[59] = 0;
    }
    static {
        POP_COUNT[60] = 2;
        POP_KIND[60 + 0] = 3;
        POP_KIND[60 + 1] = 3;
        PUSH_KIND[60] = 0;
    }
    static {
        POP_COUNT[61] = 2;
        POP_KIND[61 + 0] = 3;
        POP_KIND[61 + 1] = 3;
        PUSH_KIND[61] = 0;
    }
    static {
        POP_COUNT[62] = 2;
        POP_KIND[62 + 0] = 3;
        POP_KIND[62 + 1] = 3;
        PUSH_KIND[62] = 0;
    }
    static {
        POP_COUNT[63] = 2;
        POP_KIND[63 + 0] = 3;
        POP_KIND[63 + 1] = 3;
        PUSH_KIND[63] = 0;
    }
    static {
        POP_COUNT[64] = 2;
        POP_KIND[64 + 0] = 3;
        POP_KIND[64 + 1] = 3;
        PUSH_KIND[64] = 0;
    }
    static {
        POP_COUNT[65] = 2;
        POP_KIND[65 + 0] = 3;
        POP_KIND[65 + 1] = 3;
        PUSH_KIND[65] = 0;
    }
    static {
        POP_COUNT[66] = 2;
        POP_KIND[66 + 0] = 3;
        POP_KIND[66 + 1] = 3;
        PUSH_KIND[66] = 0;
    }
    static {
        POP_COUNT[67] = 1;
        POP_KIND[67 + 0] = 0;
        PUSH_KIND[67] = 0;
    }
    static {
        POP_COUNT[68] = 1;
        POP_KIND[68 + 0] = 3;
        PUSH_KIND[68] = 3;
    }
    static {
        POP_COUNT[69] = 1;
        POP_KIND[69 + 0] = 3;
        PUSH_KIND[69] = 0;
    }
    static {
        POP_COUNT[70] = 1;
        POP_KIND[70 + 0] = 3;
        PUSH_KIND[70] = 0;
    }
    static {
        POP_COUNT[71] = 1;
        POP_KIND[71 + 0] = 0;
        PUSH_KIND[71] = -1;
    }
    static {
        POP_COUNT[72] = 1;
        POP_KIND[72 + 0] = 0;
        PUSH_KIND[72] = -1;
    }
    static {
        POP_COUNT[73] = 0;
        PUSH_KIND[73] = -1;
    }
    static {
        POP_COUNT[74] = 0;
        PUSH_KIND[74] = -1;
    }
    static {
        POP_COUNT[75] = 0;
        PUSH_KIND[75] = -1;
    }
    static {
        POP_COUNT[76] = 0;
        PUSH_KIND[76] = -1;
    }
    static {
        POP_COUNT[77] = 0;
        PUSH_KIND[77] = -1;
    }
    static {
        POP_COUNT[78] = 0;
        PUSH_KIND[78] = -1;
    }
    static {
        POP_COUNT[79] = 1;
        POP_KIND[79 + 0] = 0;
        PUSH_KIND[79] = -1;
    }
    static {
        POP_COUNT[80] = 1;
        POP_KIND[80 + 0] = 3;
        PUSH_KIND[80] = -1;
    }
    static {
        POP_COUNT[81] = 1;
        POP_KIND[81 + 0] = 3;
        PUSH_KIND[81] = -1;
    }
    static {
        POP_COUNT[82] = 2;
        POP_KIND[82 + 0] = 3;
        POP_KIND[82 + 1] = 3;
        PUSH_KIND[82] = -1;
    }
    static {
        POP_COUNT[83] = 2;
        POP_KIND[83 + 0] = 3;
        POP_KIND[83 + 1] = 3;
        PUSH_KIND[83] = -1;
    }
    static {
        POP_COUNT[84] = 2;
        POP_KIND[84 + 0] = 3;
        POP_KIND[84 + 1] = 3;
        PUSH_KIND[84] = -1;
    }
    static {
        POP_COUNT[85] = 5;
        POP_KIND[85 + 0] = 3;
        POP_KIND[85 + 1] = 0;
        POP_KIND[85 + 2] = 0;
        POP_KIND[85 + 3] = 0;
        POP_KIND[85 + 4] = 0;
        PUSH_KIND[85] = -1;
    }
    static {
        POP_COUNT[86] = 5;
        POP_KIND[86 + 0] = 3;
        POP_KIND[86 + 1] = 0;
        POP_KIND[86 + 2] = 0;
        POP_KIND[86 + 3] = 0;
        POP_KIND[86 + 4] = 0;
        PUSH_KIND[86] = -1;
    }
    static {
        POP_COUNT[87] = 2;
        POP_KIND[87 + 0] = 0;
        POP_KIND[87 + 1] = 0;
        PUSH_KIND[87] = -1;
    }
    static {
        POP_COUNT[88] = 6;
        POP_KIND[88 + 0] = 3;
        POP_KIND[88 + 1] = 3;
        POP_KIND[88 + 2] = 3;
        POP_KIND[88 + 3] = 3;
        POP_KIND[88 + 4] = 0;
        POP_KIND[88 + 5] = 0;
        PUSH_KIND[88] = -1;
    }
    static {
        POP_COUNT[89] = 6;
        POP_KIND[89 + 0] = 3;
        POP_KIND[89 + 1] = 3;
        POP_KIND[89 + 2] = 3;
        POP_KIND[89 + 3] = 3;
        POP_KIND[89 + 4] = 0;
        POP_KIND[89 + 5] = 0;
        PUSH_KIND[89] = -1;
    }
    static {
        POP_COUNT[90] = 1;
        POP_KIND[90 + 0] = 0;
        PUSH_KIND[90] = -1;
    }
    static {
        POP_COUNT[91] = 3;
        POP_KIND[91 + 0] = 3;
        POP_KIND[91 + 1] = 3;
        POP_KIND[91 + 2] = 3;
        PUSH_KIND[91] = -1;
    }
    static {
        POP_COUNT[92] = 3;
        POP_KIND[92 + 0] = 3;
        POP_KIND[92 + 1] = 3;
        POP_KIND[92 + 2] = 3;
        PUSH_KIND[92] = -1;
    }
    static {
        POP_COUNT[93] = 0;
        PUSH_KIND[93] = -1;
    }
    static {
        POP_COUNT[94] = 0;
        PUSH_KIND[94] = 0;
    }
    static {
        POP_COUNT[95] = 1;
        POP_KIND[95 + 0] = 0;
        PUSH_KIND[95] = -1;
    }
    static {
        POP_COUNT[96] = 1;
        POP_KIND[96 + 0] = 0;
        PUSH_KIND[96] = -1;
    }
    static {
        POP_COUNT[97] = 1;
        POP_KIND[97 + 0] = 3;
        PUSH_KIND[97] = -1;
    }
    static {
        POP_COUNT[98] = 5;
        POP_KIND[98 + 0] = 0;
        POP_KIND[98 + 1] = 0;
        POP_KIND[98 + 2] = 0;
        POP_KIND[98 + 3] = 0;
        POP_KIND[98 + 4] = 1;
        PUSH_KIND[98] = -1;
    }
    static {
        POP_COUNT[99] = 1;
        POP_KIND[99 + 0] = 1;
        PUSH_KIND[99] = -1;
    }
    static {
        POP_COUNT[100] = 1;
        POP_KIND[100 + 0] = 1;
        PUSH_KIND[100] = -1;
    }
    static {
        POP_COUNT[101] = 5;
        POP_KIND[101 + 0] = 0;
        POP_KIND[101 + 1] = 0;
        POP_KIND[101 + 2] = 0;
        POP_KIND[101 + 3] = 0;
        POP_KIND[101 + 4] = 1;
        PUSH_KIND[101] = -1;
    }
    static {
        POP_COUNT[102] = 5;
        POP_KIND[102 + 0] = 0;
        POP_KIND[102 + 1] = 0;
        POP_KIND[102 + 2] = 0;
        POP_KIND[102 + 3] = 0;
        POP_KIND[102 + 4] = 1;
        PUSH_KIND[102] = -1;
    }
    static {
        POP_COUNT[103] = 0;
        PUSH_KIND[103] = -1;
    }
    static {
        POP_COUNT[104] = 3;
        POP_KIND[104 + 0] = 3;
        POP_KIND[104 + 1] = 3;
        POP_KIND[104 + 2] = 3;
        PUSH_KIND[104] = -1;
    }
    static {
        POP_COUNT[105] = 7;
        POP_KIND[105 + 0] = 3;
        POP_KIND[105 + 1] = 3;
        POP_KIND[105 + 2] = 3;
        POP_KIND[105 + 3] = 0;
        POP_KIND[105 + 4] = 0;
        POP_KIND[105 + 5] = 0;
        POP_KIND[105 + 6] = 0;
        PUSH_KIND[105] = -1;
    }
    static {
        POP_COUNT[106] = 1;
        POP_KIND[106 + 0] = 1;
        PUSH_KIND[106] = -1;
    }
    static {
        POP_COUNT[107] = 1;
        POP_KIND[107 + 0] = 1;
        PUSH_KIND[107] = -1;
    }
    static {
        POP_COUNT[108] = 5;
        POP_KIND[108 + 0] = 0;
        POP_KIND[108 + 1] = 0;
        POP_KIND[108 + 2] = 0;
        POP_KIND[108 + 3] = 0;
        POP_KIND[108 + 4] = 1;
        PUSH_KIND[108] = -1;
    }
    static {
        POP_COUNT[109] = 1;
        POP_KIND[109 + 0] = 1;
        PUSH_KIND[109] = -1;
    }
    static {
        POP_COUNT[110] = 1;
        POP_KIND[110 + 0] = 1;
        PUSH_KIND[110] = -1;
    }
    static {
        POP_COUNT[111] = 1;
        POP_KIND[111 + 0] = 0;
        PUSH_KIND[111] = -1;
    }
    static {
        POP_COUNT[112] = 1;
        POP_KIND[112 + 0] = 0;
        PUSH_KIND[112] = -1;
    }
    static {
        POP_COUNT[113] = 2;
        POP_KIND[113 + 0] = 0;
        POP_KIND[113 + 1] = 0;
        PUSH_KIND[113] = -1;
    }
    static {
        POP_COUNT[114] = 1;
        POP_KIND[114 + 0] = 3;
        PUSH_KIND[114] = -1;
    }
    static {
        POP_COUNT[115] = 6;
        POP_KIND[115 + 0] = 0;
        POP_KIND[115 + 1] = 0;
        POP_KIND[115 + 2] = 0;
        POP_KIND[115 + 3] = 0;
        POP_KIND[115 + 4] = 0;
        POP_KIND[115 + 5] = 1;
        PUSH_KIND[115] = -1;
    }
    static {
        POP_COUNT[116] = 1;
        POP_KIND[116 + 0] = 0;
        PUSH_KIND[116] = -1;
    }
    static {
        POP_COUNT[117] = 1;
        POP_KIND[117 + 0] = 0;
        PUSH_KIND[117] = -1;
    }
    static {
        POP_COUNT[118] = 1;
        POP_KIND[118 + 0] = 0;
        PUSH_KIND[118] = -1;
    }
    static {
        POP_COUNT[119] = 3;
        POP_KIND[119 + 0] = 3;
        POP_KIND[119 + 1] = 3;
        POP_KIND[119 + 2] = 3;
        PUSH_KIND[119] = -1;
    }
    static {
        POP_COUNT[120] = 3;
        POP_KIND[120 + 0] = 3;
        POP_KIND[120 + 1] = 3;
        POP_KIND[120 + 2] = 3;
        PUSH_KIND[120] = -1;
    }
    static {
        POP_COUNT[121] = 0;
        PUSH_KIND[121] = -1;
    }
    static {
        POP_COUNT[122] = 1;
        POP_KIND[122 + 0] = 1;
        PUSH_KIND[122] = -1;
    }
    static {
        POP_COUNT[123] = 1;
        POP_KIND[123 + 0] = 0;
        PUSH_KIND[123] = -1;
    }
    static {
        POP_COUNT[124] = 5;
        POP_KIND[124 + 0] = 0;
        POP_KIND[124 + 1] = 0;
        POP_KIND[124 + 2] = 0;
        POP_KIND[124 + 3] = 0;
        POP_KIND[124 + 4] = 1;
        PUSH_KIND[124] = -1;
    }
    static {
        POP_COUNT[127] = 1;
        POP_KIND[127 + 0] = 0;
        PUSH_KIND[127] = -1;
    }
    static {
        POP_COUNT[128] = 2;
        POP_KIND[128 + 0] = 0;
        POP_KIND[128 + 1] = 1;
        PUSH_KIND[128] = -1;
    }
    static {
        POP_COUNT[129] = 5;
        POP_KIND[129 + 0] = 0;
        POP_KIND[129 + 1] = 0;
        POP_KIND[129 + 2] = 0;
        POP_KIND[129 + 3] = 0;
        POP_KIND[129 + 4] = 1;
        PUSH_KIND[129] = -1;
    }
    static {
        POP_COUNT[130] = 3;
        POP_KIND[130 + 0] = 0;
        POP_KIND[130 + 1] = 0;
        POP_KIND[130 + 2] = 1;
        PUSH_KIND[130] = -1;
    }
    static {
        POP_COUNT[131] = 1;
        POP_KIND[131 + 0] = 3;
        PUSH_KIND[131] = -1;
    }
    static {
        POP_COUNT[132] = 2;
        POP_KIND[132 + 0] = 0;
        POP_KIND[132 + 1] = 1;
        PUSH_KIND[132] = -1;
    }
    static {
        POP_COUNT[133] = 1;
        POP_KIND[133 + 0] = 1;
        PUSH_KIND[133] = 0;
    }
    static {
        POP_COUNT[134] = 5;
        POP_KIND[134 + 0] = 0;
        POP_KIND[134 + 1] = 0;
        POP_KIND[134 + 2] = 0;
        POP_KIND[134 + 3] = 0;
        POP_KIND[134 + 4] = 1;
        PUSH_KIND[134] = -1;
    }
    static {
        POP_COUNT[135] = 2;
        POP_KIND[135 + 0] = 1;
        POP_KIND[135 + 1] = 1;
        PUSH_KIND[135] = -1;
    }
    static {
        POP_COUNT[136] = 2;
        POP_KIND[136 + 0] = 0;
        POP_KIND[136 + 1] = 1;
        PUSH_KIND[136] = -1;
    }
    static {
        POP_COUNT[137] = 4;
        POP_KIND[137 + 0] = 3;
        POP_KIND[137 + 1] = 3;
        POP_KIND[137 + 2] = 3;
        POP_KIND[137 + 3] = 1;
        PUSH_KIND[137] = -1;
    }
    static {
        POP_COUNT[138] = 5;
        POP_KIND[138 + 0] = 0;
        POP_KIND[138 + 1] = 0;
        POP_KIND[138 + 2] = 0;
        POP_KIND[138 + 3] = 0;
        POP_KIND[138 + 4] = 1;
        PUSH_KIND[138] = -1;
    }
    static {
        POP_COUNT[139] = 3;
        POP_KIND[139 + 0] = 0;
        POP_KIND[139 + 1] = 0;
        POP_KIND[139 + 2] = 1;
        PUSH_KIND[139] = -1;
    }
    static {
        POP_COUNT[140] = 1;
        POP_KIND[140 + 0] = 3;
        PUSH_KIND[140] = -1;
    }
    static {
        POP_COUNT[141] = 0;
        PUSH_KIND[141] = -1;
    }
    static {
        POP_COUNT[142] = 0;
        PUSH_KIND[142] = -1;
    }
    static {
        POP_COUNT[143] = 3;
        POP_KIND[143 + 0] = 0;
        POP_KIND[143 + 1] = 0;
        POP_KIND[143 + 2] = 1;
        PUSH_KIND[143] = -1;
    }
    static {
        POP_COUNT[144] = 1;
        POP_KIND[144 + 0] = 0;
        PUSH_KIND[144] = -1;
    }
    static {
        POP_COUNT[145] = 0;
        PUSH_KIND[145] = -1;
    }
    static {
        POP_COUNT[146] = 2;
        POP_KIND[146 + 0] = 0;
        POP_KIND[146 + 1] = 1;
        PUSH_KIND[146] = -1;
    }
    static {
        POP_COUNT[147] = 2;
        POP_KIND[147 + 0] = 0;
        POP_KIND[147 + 1] = 1;
        PUSH_KIND[147] = -1;
    }
    static {
        POP_COUNT[148] = 1;
        POP_KIND[148 + 0] = 1;
        PUSH_KIND[148] = -1;
    }
    static {
        POP_COUNT[149] = 1;
        POP_KIND[149 + 0] = 1;
        PUSH_KIND[149] = -1;
    }
    static {
        POP_COUNT[150] = 2;
        POP_KIND[150 + 0] = 0;
        POP_KIND[150 + 1] = 0;
        PUSH_KIND[150] = -1;
    }
    static {
        POP_COUNT[151] = 1;
        POP_KIND[151 + 0] = 0;
        PUSH_KIND[151] = -1;
    }
    static {
        POP_COUNT[152] = 2;
        POP_KIND[152 + 0] = 0;
        POP_KIND[152 + 1] = 1;
        PUSH_KIND[152] = 0;
    }
    static {
        POP_COUNT[153] = 1;
        POP_KIND[153 + 0] = 0;
        PUSH_KIND[153] = -1;
    }
    static {
        POP_COUNT[154] = 1;
        POP_KIND[154 + 0] = 0;
        PUSH_KIND[154] = -1;
    }
    static {
        POP_COUNT[155] = 1;
        POP_KIND[155 + 0] = 0;
        PUSH_KIND[155] = -1;
    }
    static {
        POP_COUNT[156] = 1;
        POP_KIND[156 + 0] = 0;
        PUSH_KIND[156] = -1;
    }
    static {
        POP_COUNT[157] = 1;
        POP_KIND[157 + 0] = 0;
        PUSH_KIND[157] = -1;
    }
    static {
        POP_COUNT[158] = 1;
        POP_KIND[158 + 0] = 0;
        PUSH_KIND[158] = -1;
    }
    static {
        POP_COUNT[159] = 2;
        POP_KIND[159 + 0] = 1;
        POP_KIND[159 + 1] = 1;
        PUSH_KIND[159] = 3;
    }
    static {
        POP_COUNT[160] = 2;
        POP_KIND[160 + 0] = 0;
        POP_KIND[160 + 1] = 3;
        PUSH_KIND[160] = 3;
    }
    static {
        POP_COUNT[161] = 1;
        POP_KIND[161 + 0] = 0;
        PUSH_KIND[161] = -1;
    }
    static {
        POP_COUNT[162] = 1;
        POP_KIND[162 + 0] = 0;
        PUSH_KIND[162] = -1;
    }
    static {
        POP_COUNT[163] = 1;
        POP_KIND[163 + 0] = 1;
        PUSH_KIND[163] = -1;
    }
    static {
        POP_COUNT[164] = 1;
        POP_KIND[164 + 0] = 2;
        PUSH_KIND[164] = 1;
    }
    static {
        POP_COUNT[165] = 2;
        POP_KIND[165 + 0] = 0;
        POP_KIND[165 + 1] = 2;
        PUSH_KIND[165] = 1;
    }
    static {
        POP_COUNT[166] = 1;
        POP_KIND[166 + 0] = 0;
        PUSH_KIND[166] = 0;
    }
    static {
        POP_COUNT[167] = 3;
        POP_KIND[167 + 0] = 0;
        POP_KIND[167 + 1] = 0;
        POP_KIND[167 + 2] = 0;
        PUSH_KIND[167] = 2;
    }
    static {
        POP_COUNT[168] = 4;
        POP_KIND[168 + 0] = 0;
        POP_KIND[168 + 1] = 0;
        POP_KIND[168 + 2] = 0;
        POP_KIND[168 + 3] = 0;
        PUSH_KIND[168] = -1;
    }
    static {
        POP_COUNT[169] = 4;
        POP_KIND[169 + 0] = 0;
        POP_KIND[169 + 1] = 0;
        POP_KIND[169 + 2] = 0;
        POP_KIND[169 + 3] = 0;
        PUSH_KIND[169] = -1;
    }
    static {
        POP_COUNT[170] = 2;
        POP_KIND[170 + 0] = 0;
        POP_KIND[170 + 1] = 2;
        PUSH_KIND[170] = 0;
    }
    static {
        POP_COUNT[171] = 2;
        POP_KIND[171 + 0] = 0;
        POP_KIND[171 + 1] = 2;
        PUSH_KIND[171] = 0;
    }
    static {
        POP_COUNT[172] = 3;
        POP_KIND[172 + 0] = 0;
        POP_KIND[172 + 1] = 0;
        POP_KIND[172 + 2] = 2;
        PUSH_KIND[172] = 0;
    }
    static {
        POP_COUNT[173] = 3;
        POP_KIND[173 + 0] = 0;
        POP_KIND[173 + 1] = 0;
        POP_KIND[173 + 2] = 0;
        PUSH_KIND[173] = 0;
    }
    static {
        POP_COUNT[174] = 3;
        POP_KIND[174 + 0] = 0;
        POP_KIND[174 + 1] = 0;
        POP_KIND[174 + 2] = 2;
        PUSH_KIND[174] = 0;
    }
    static {
        POP_COUNT[175] = 1;
        POP_KIND[175 + 0] = 0;
        PUSH_KIND[175] = 0;
    }
    static {
        POP_COUNT[176] = 1;
        POP_KIND[176 + 0] = 0;
        PUSH_KIND[176] = 0;
    }
    static {
        POP_COUNT[177] = 1;
        POP_KIND[177 + 0] = 0;
        PUSH_KIND[177] = 0;
    }
    static {
        POP_COUNT[178] = 3;
        POP_KIND[178 + 0] = 0;
        POP_KIND[178 + 1] = 0;
        POP_KIND[178 + 2] = 0;
        PUSH_KIND[178] = -1;
    }
    static {
        POP_COUNT[179] = 3;
        POP_KIND[179 + 0] = 0;
        POP_KIND[179 + 1] = 0;
        POP_KIND[179 + 2] = 0;
        PUSH_KIND[179] = 0;
    }
    static {
        POP_COUNT[180] = 2;
        POP_KIND[180 + 0] = 0;
        POP_KIND[180 + 1] = 0;
        PUSH_KIND[180] = 0;
    }
    static {
        POP_COUNT[181] = 2;
        POP_KIND[181 + 0] = 0;
        POP_KIND[181 + 1] = 0;
        PUSH_KIND[181] = 0;
    }
    static {
        POP_COUNT[182] = 3;
        POP_KIND[182 + 0] = 0;
        POP_KIND[182 + 1] = 0;
        POP_KIND[182 + 2] = 2;
        PUSH_KIND[182] = 0;
    }
    static {
        POP_COUNT[183] = 4;
        POP_KIND[183 + 0] = 0;
        POP_KIND[183 + 1] = 0;
        POP_KIND[183 + 2] = 0;
        POP_KIND[183 + 3] = 0;
        PUSH_KIND[183] = 2;
    }
    static {
        POP_COUNT[184] = 1;
        POP_KIND[184 + 0] = 1;
        PUSH_KIND[184] = -1;
    }
    static {
        POP_COUNT[185] = 0;
        PUSH_KIND[185] = -1;
    }
    static {
        POP_COUNT[186] = 2;
        POP_KIND[186 + 0] = 0;
        POP_KIND[186 + 1] = 2;
        PUSH_KIND[186] = 0;
    }
    static {
        POP_COUNT[187] = 3;
        POP_KIND[187 + 0] = 0;
        POP_KIND[187 + 1] = 0;
        POP_KIND[187 + 2] = 0;
        PUSH_KIND[187] = 0;
    }
    static {
        POP_COUNT[188] = 0;
        PUSH_KIND[188] = 2;
    }
    static {
        POP_COUNT[189] = 0;
        PUSH_KIND[189] = 2;
    }
    static {
        POP_COUNT[190] = 0;
        PUSH_KIND[190] = 2;
    }
    static {
        POP_COUNT[191] = 0;
        PUSH_KIND[191] = 2;
    }
    static {
        POP_COUNT[192] = 0;
        PUSH_KIND[192] = 2;
    }
    static {
        POP_COUNT[193] = 1;
        POP_KIND[193 + 0] = 0;
        PUSH_KIND[193] = 3;
    }
    static {
        POP_COUNT[194] = 2;
        POP_KIND[194 + 0] = 3;
        POP_KIND[194 + 1] = 0;
        PUSH_KIND[194] = -1;
    }
    static {
        POP_COUNT[195] = 1;
        POP_KIND[195 + 0] = 0;
        PUSH_KIND[195] = 3;
    }
    static {
        POP_COUNT[196] = 2;
        POP_KIND[196 + 0] = 3;
        POP_KIND[196 + 1] = 0;
        PUSH_KIND[196] = -1;
    }
    static {
        POP_COUNT[197] = 1;
        POP_KIND[197 + 0] = 0;
        PUSH_KIND[197] = 3;
    }
    static {
        POP_COUNT[198] = 2;
        POP_KIND[198 + 0] = 3;
        POP_KIND[198 + 1] = 0;
        PUSH_KIND[198] = -1;
    }
    static {
        POP_COUNT[199] = 0;
        PUSH_KIND[199] = 0;
    }
    static {
        POP_COUNT[200] = 1;
        POP_KIND[200 + 0] = 2;
        PUSH_KIND[200] = 0;
    }
    static {
        POP_COUNT[201] = 1;
        POP_KIND[201 + 0] = 2;
        PUSH_KIND[201] = 0;
    }
    static {
        POP_COUNT[202] = 2;
        POP_KIND[202 + 0] = 0;
        POP_KIND[202 + 1] = 2;
        PUSH_KIND[202] = 0;
    }
    static {
        POP_COUNT[203] = 3;
        POP_KIND[203 + 0] = 0;
        POP_KIND[203 + 1] = 0;
        POP_KIND[203 + 2] = 2;
        PUSH_KIND[203] = 0;
    }
    static {
        POP_COUNT[204] = 1;
        POP_KIND[204 + 0] = 2;
        PUSH_KIND[204] = -1;
    }
    static {
        POP_COUNT[205] = 1;
        POP_KIND[205 + 0] = 2;
        PUSH_KIND[205] = -1;
    }
    static {
        POP_COUNT[206] = 3;
        POP_KIND[206 + 0] = 0;
        POP_KIND[206 + 1] = 0;
        POP_KIND[206 + 2] = 2;
        PUSH_KIND[206] = -1;
    }
    static {
        POP_COUNT[207] = 5;
        POP_KIND[207 + 0] = 0;
        POP_KIND[207 + 1] = 0;
        POP_KIND[207 + 2] = 0;
        POP_KIND[207 + 3] = 0;
        POP_KIND[207 + 4] = 0;
        PUSH_KIND[207] = 0;
    }
    static {
        POP_COUNT[208] = 8;
        POP_KIND[208 + 0] = 0;
        POP_KIND[208 + 1] = 0;
        POP_KIND[208 + 2] = 0;
        POP_KIND[208 + 3] = 0;
        POP_KIND[208 + 4] = 0;
        POP_KIND[208 + 5] = 0;
        POP_KIND[208 + 6] = 0;
        POP_KIND[208 + 7] = 2;
        PUSH_KIND[208] = -1;
    }
    static {
        POP_COUNT[209] = 0;
        PUSH_KIND[209] = -1;
    }
    static {
        POP_COUNT[210] = 2;
        POP_KIND[210 + 0] = 0;
        POP_KIND[210 + 1] = 0;
        PUSH_KIND[210] = 0;
    }
    static {
        POP_COUNT[211] = 2;
        POP_KIND[211 + 0] = 2;
        POP_KIND[211 + 1] = 2;
        PUSH_KIND[211] = 0;
    }
    static {
        POP_COUNT[212] = 1;
        POP_KIND[212 + 0] = 2;
        PUSH_KIND[212] = 0;
    }
    static {
        POP_COUNT[213] = 3;
        POP_KIND[213 + 0] = 0;
        POP_KIND[213 + 1] = 0;
        POP_KIND[213 + 2] = 0;
        PUSH_KIND[213] = 0;
    }
    static {
        POP_COUNT[214] = 1;
        POP_KIND[214 + 0] = 2;
        PUSH_KIND[214] = -1;
    }
    static {
        POP_COUNT[215] = 1;
        POP_KIND[215 + 0] = 2;
        PUSH_KIND[215] = -1;
    }
    static {
        POP_COUNT[216] = 2;
        POP_KIND[216 + 0] = 2;
        POP_KIND[216 + 1] = 2;
        PUSH_KIND[216] = -1;
    }
    static {
        POP_COUNT[217] = 2;
        POP_KIND[217 + 0] = 2;
        POP_KIND[217 + 1] = 2;
        PUSH_KIND[217] = -1;
    }
    static {
        POP_COUNT[218] = 2;
        POP_KIND[218 + 0] = 2;
        POP_KIND[218 + 1] = 2;
        PUSH_KIND[218] = -1;
    }
    static {
        POP_COUNT[219] = 1;
        POP_KIND[219 + 0] = 2;
        PUSH_KIND[219] = -1;
    }
    static {
        POP_COUNT[220] = 2;
        POP_KIND[220 + 0] = 2;
        POP_KIND[220 + 1] = 2;
        PUSH_KIND[220] = 0;
    }
    static {
        POP_COUNT[221] = 8;
        POP_KIND[221 + 0] = 0;
        POP_KIND[221 + 1] = 0;
        POP_KIND[221 + 2] = 0;
        POP_KIND[221 + 3] = 0;
        POP_KIND[221 + 4] = 0;
        POP_KIND[221 + 5] = 0;
        POP_KIND[221 + 6] = 0;
        POP_KIND[221 + 7] = 2;
        PUSH_KIND[221] = -1;
    }
    static {
        POP_COUNT[222] = 5;
        POP_KIND[222 + 0] = 0;
        POP_KIND[222 + 1] = 0;
        POP_KIND[222 + 2] = 0;
        POP_KIND[222 + 3] = 2;
        POP_KIND[222 + 4] = 0;
        PUSH_KIND[222] = -1;
    }
    static {
        POP_COUNT[223] = 0;
        PUSH_KIND[223] = -1;
    }
    static {
        POP_COUNT[224] = 1;
        POP_KIND[224 + 0] = 0;
        PUSH_KIND[224] = -1;
    }
    static {
        POP_COUNT[225] = 4;
        POP_KIND[225 + 0] = 3;
        POP_KIND[225 + 1] = 3;
        POP_KIND[225 + 2] = 3;
        POP_KIND[225 + 3] = 0;
        PUSH_KIND[225] = 3;
    }
    static {
        POP_COUNT[226] = 2;
        POP_KIND[226 + 0] = 1;
        POP_KIND[226 + 1] = 0;
        PUSH_KIND[226] = -1;
    }
    static {
        POP_COUNT[227] = 2;
        POP_KIND[227 + 0] = 0;
        POP_KIND[227 + 1] = 2;
        PUSH_KIND[227] = -1;
    }
    static {
        POP_COUNT[228] = 2;
        POP_KIND[228 + 0] = 0;
        POP_KIND[228 + 1] = 3;
        PUSH_KIND[228] = -1;
    }
    static {
        POP_COUNT[229] = 3;
        POP_KIND[229 + 0] = 0;
        POP_KIND[229 + 1] = 0;
        POP_KIND[229 + 2] = 0;
        PUSH_KIND[229] = -1;
    }
    static {
        POP_COUNT[230] = 5;
        POP_KIND[230 + 0] = 0;
        POP_KIND[230 + 1] = 0;
        POP_KIND[230 + 2] = 0;
        POP_KIND[230 + 3] = 0;
        POP_KIND[230 + 4] = 0;
        PUSH_KIND[230] = -1;
    }
    static {
        POP_COUNT[231] = 1;
        POP_KIND[231 + 0] = 2;
        PUSH_KIND[231] = 0;
    }
    static {
        POP_COUNT[232] = 2;
        POP_KIND[232 + 0] = 0;
        POP_KIND[232 + 1] = 2;
        PUSH_KIND[232] = 0;
    }
    static {
        POP_COUNT[233] = 1;
        POP_KIND[233 + 0] = 0;
        PUSH_KIND[233] = -1;
    }
    static {
        POP_COUNT[234] = 0;
        PUSH_KIND[234] = 0;
    }
    static {
        POP_COUNT[235] = 0;
        PUSH_KIND[235] = 0;
    }
    static {
        POP_COUNT[236] = 1;
        POP_KIND[236 + 0] = 2;
        PUSH_KIND[236] = 0;
    }
    static {
        POP_COUNT[237] = 2;
        POP_KIND[237 + 0] = 0;
        POP_KIND[237 + 1] = 2;
        PUSH_KIND[237] = -1;
    }
    static {
        POP_COUNT[238] = 2;
        POP_KIND[238 + 0] = 0;
        POP_KIND[238 + 1] = 0;
        PUSH_KIND[238] = -1;
    }
    static {
        POP_COUNT[239] = 3;
        POP_KIND[239 + 0] = 0;
        POP_KIND[239 + 1] = 0;
        POP_KIND[239 + 2] = 2;
        PUSH_KIND[239] = -1;
    }
    static {
        POP_COUNT[240] = 3;
        POP_KIND[240 + 0] = 0;
        POP_KIND[240 + 1] = 0;
        POP_KIND[240 + 2] = 2;
        PUSH_KIND[240] = -1;
    }
    static {
        POP_COUNT[241] = 1;
        POP_KIND[241 + 0] = 2;
        PUSH_KIND[241] = -1;
    }
    static {
        POP_COUNT[242] = 1;
        POP_KIND[242 + 0] = 0;
        PUSH_KIND[242] = 0;
    }
    static {
        POP_COUNT[243] = 3;
        POP_KIND[243 + 0] = 0;
        POP_KIND[243 + 1] = 2;
        POP_KIND[243 + 2] = 0;
        PUSH_KIND[243] = 0;
    }
    static {
        POP_COUNT[244] = 1;
        POP_KIND[244 + 0] = 2;
        PUSH_KIND[244] = -1;
    }
    static {
        POP_COUNT[245] = 2;
        POP_KIND[245 + 0] = 2;
        POP_KIND[245 + 1] = 2;
        PUSH_KIND[245] = 0;
    }
    static {
        POP_COUNT[246] = 0;
        PUSH_KIND[246] = 0;
    }
    static {
        POP_COUNT[247] = 0;
        PUSH_KIND[247] = 0;
    }
    static {
        POP_COUNT[248] = 1;
        POP_KIND[248 + 0] = 0;
        PUSH_KIND[248] = 0;
    }
    static {
        POP_COUNT[249] = 0;
        PUSH_KIND[249] = -1;
    }
    static {
        POP_COUNT[250] = 0;
        PUSH_KIND[250] = 0;
    }
    static {
        POP_COUNT[251] = 1;
        POP_KIND[251 + 0] = 2;
        PUSH_KIND[251] = 0;
    }
    static {
        POP_COUNT[252] = 1;
        POP_KIND[252 + 0] = 0;
        PUSH_KIND[252] = -1;
    }
    static {
        POP_COUNT[253] = 2;
        POP_KIND[253 + 0] = 0;
        POP_KIND[253 + 1] = 2;
        PUSH_KIND[253] = -1;
    }
    static {
        POP_COUNT[254] = 2;
        POP_KIND[254 + 0] = 0;
        POP_KIND[254 + 1] = 2;
        PUSH_KIND[254] = -1;
    }
    static {
        POP_COUNT[255] = 3;
        POP_KIND[255 + 0] = 0;
        POP_KIND[255 + 1] = 0;
        POP_KIND[255 + 2] = 2;
        PUSH_KIND[255] = 0;
    }
    static {
        POP_COUNT[256] = 1;
        POP_KIND[256 + 0] = 2;
        PUSH_KIND[256] = 0;
    }
    static {
        POP_COUNT[257] = 0;
        PUSH_KIND[257] = 0;
    }
    static {
        POP_COUNT[258] = 4;
        POP_KIND[258 + 0] = 0;
        POP_KIND[258 + 1] = 0;
        POP_KIND[258 + 2] = 0;
        POP_KIND[258 + 3] = 2;
        PUSH_KIND[258] = 0;
    }
    static {
        POP_COUNT[259] = 4;
        POP_KIND[259 + 0] = 0;
        POP_KIND[259 + 1] = 0;
        POP_KIND[259 + 2] = 0;
        POP_KIND[259 + 3] = 2;
        PUSH_KIND[259] = 0;
    }
    static {
        POP_COUNT[260] = 2;
        POP_KIND[260 + 0] = 0;
        POP_KIND[260 + 1] = 0;
        PUSH_KIND[260] = 3;
    }
    static {
        POP_COUNT[261] = 2;
        POP_KIND[261 + 0] = 0;
        POP_KIND[261 + 1] = 0;
        PUSH_KIND[261] = 1;
    }
    static {
        POP_COUNT[262] = 2;
        POP_KIND[262 + 0] = 0;
        POP_KIND[262 + 1] = 2;
        PUSH_KIND[262] = 3;
    }
    static {
        POP_COUNT[263] = 3;
        POP_KIND[263 + 0] = 0;
        POP_KIND[263 + 1] = 0;
        POP_KIND[263 + 2] = 2;
        PUSH_KIND[263] = -1;
    }
    static {
        POP_COUNT[264] = 0;
        PUSH_KIND[264] = -1;
    }
    static {
        POP_COUNT[265] = 3;
        POP_KIND[265 + 0] = 0;
        POP_KIND[265 + 1] = 0;
        POP_KIND[265 + 2] = 2;
        PUSH_KIND[265] = 2;
    }
    static {
        POP_COUNT[266] = 2;
        POP_KIND[266 + 0] = 0;
        POP_KIND[266 + 1] = 3;
        PUSH_KIND[266] = -1;
    }
    static {
        POP_COUNT[267] = 2;
        POP_KIND[267 + 0] = 3;
        POP_KIND[267 + 1] = 0;
        PUSH_KIND[267] = 0;
    }
    static {
        POP_COUNT[268] = 4;
        POP_KIND[268 + 0] = 3;
        POP_KIND[268 + 1] = 0;
        POP_KIND[268 + 2] = 0;
        POP_KIND[268 + 3] = 2;
        PUSH_KIND[268] = -1;
    }
    static {
        POP_COUNT[269] = 2;
        POP_KIND[269 + 0] = 0;
        POP_KIND[269 + 1] = 2;
        PUSH_KIND[269] = 2;
    }
    static {
        POP_COUNT[270] = 2;
        POP_KIND[270 + 0] = 3;
        POP_KIND[270 + 1] = 0;
        PUSH_KIND[270] = -1;
    }
    static {
        POP_COUNT[271] = 3;
        POP_KIND[271 + 0] = 0;
        POP_KIND[271 + 1] = 0;
        POP_KIND[271 + 2] = 2;
        PUSH_KIND[271] = -1;
    }
    static {
        POP_COUNT[272] = 3;
        POP_KIND[272 + 0] = 0;
        POP_KIND[272 + 1] = 0;
        POP_KIND[272 + 2] = 2;
        PUSH_KIND[272] = -1;
    }
    static {
        POP_COUNT[273] = 3;
        POP_KIND[273 + 0] = 0;
        POP_KIND[273 + 1] = 2;
        POP_KIND[273 + 2] = 2;
        PUSH_KIND[273] = -1;
    }
    static {
        POP_COUNT[274] = 3;
        POP_KIND[274 + 0] = 0;
        POP_KIND[274 + 1] = 2;
        POP_KIND[274 + 2] = 2;
        PUSH_KIND[274] = -1;
    }
    static {
        POP_COUNT[275] = 3;
        POP_KIND[275 + 0] = 0;
        POP_KIND[275 + 1] = 0;
        POP_KIND[275 + 2] = 2;
        PUSH_KIND[275] = -1;
    }
    static {
        POP_COUNT[276] = 3;
        POP_KIND[276 + 0] = 0;
        POP_KIND[276 + 1] = 0;
        POP_KIND[276 + 2] = 2;
        PUSH_KIND[276] = -1;
    }
    static {
        POP_COUNT[277] = 1;
        POP_KIND[277 + 0] = 0;
        PUSH_KIND[277] = -1;
    }
    static {
        POP_COUNT[278] = 3;
        POP_KIND[278 + 0] = 0;
        POP_KIND[278 + 1] = 2;
        POP_KIND[278 + 2] = 2;
        PUSH_KIND[278] = -1;
    }
    static {
        POP_COUNT[279] = 3;
        POP_KIND[279 + 0] = 0;
        POP_KIND[279 + 1] = 2;
        POP_KIND[279 + 2] = 2;
        PUSH_KIND[279] = 0;
    }
    static {
        POP_COUNT[280] = 0;
        PUSH_KIND[280] = 0;
    }
    static {
        POP_COUNT[281] = 0;
        PUSH_KIND[281] = 0;
    }
    static {
        POP_COUNT[282] = 3;
        POP_KIND[282 + 0] = 0;
        POP_KIND[282 + 1] = 0;
        POP_KIND[282 + 2] = 0;
        PUSH_KIND[282] = -1;
    }
    static {
        POP_COUNT[283] = 0;
        PUSH_KIND[283] = 0;
    }
    static {
        POP_COUNT[284] = 0;
        PUSH_KIND[284] = -1;
    }
    static {
        POP_COUNT[285] = 0;
        PUSH_KIND[285] = -1;
    }
    static {
        POP_COUNT[286] = 2;
        POP_KIND[286 + 0] = 3;
        POP_KIND[286 + 1] = 0;
        PUSH_KIND[286] = -1;
    }
    static {
        POP_COUNT[287] = 4;
        POP_KIND[287 + 0] = 0;
        POP_KIND[287 + 1] = 3;
        POP_KIND[287 + 2] = 3;
        POP_KIND[287 + 3] = 0;
        PUSH_KIND[287] = -1;
    }
    static {
        POP_COUNT[288] = 3;
        POP_KIND[288 + 0] = 0;
        POP_KIND[288 + 1] = 3;
        POP_KIND[288 + 2] = 0;
        PUSH_KIND[288] = -1;
    }
    static {
        POP_COUNT[289] = 5;
        POP_KIND[289 + 0] = 0;
        POP_KIND[289 + 1] = 3;
        POP_KIND[289 + 2] = 3;
        POP_KIND[289 + 3] = 0;
        POP_KIND[289 + 4] = 0;
        PUSH_KIND[289] = -1;
    }
    static {
        POP_COUNT[290] = 2;
        POP_KIND[290 + 0] = 0;
        POP_KIND[290 + 1] = 2;
        PUSH_KIND[290] = -1;
    }
    static {
        POP_COUNT[291] = 1;
        POP_KIND[291 + 0] = 2;
        PUSH_KIND[291] = 0;
    }
    static {
        POP_COUNT[292] = 1;
        POP_KIND[292 + 0] = 2;
        PUSH_KIND[292] = -1;
    }
    static {
        POP_COUNT[293] = 1;
        POP_KIND[293 + 0] = 2;
        PUSH_KIND[293] = -1;
    }
    static {
        POP_COUNT[294] = 2;
        POP_KIND[294 + 0] = 0;
        POP_KIND[294 + 1] = 2;
        PUSH_KIND[294] = -1;
    }
    static {
        POP_COUNT[295] = 2;
        POP_KIND[295 + 0] = 0;
        POP_KIND[295 + 1] = 2;
        PUSH_KIND[295] = -1;
    }
    static {
        POP_COUNT[296] = 0;
        PUSH_KIND[296] = 0;
    }
    static {
        POP_COUNT[297] = 1;
        POP_KIND[297 + 0] = 0;
        PUSH_KIND[297] = -1;
    }
    static {
        POP_COUNT[298] = 0;
        PUSH_KIND[298] = 0;
    }
    static {
        POP_COUNT[299] = 0;
        PUSH_KIND[299] = 0;
    }
    static {
        POP_COUNT[300] = 0;
        PUSH_KIND[300] = -1;
    }
    static {
        POP_COUNT[301] = 1;
        POP_KIND[301 + 0] = 2;
        PUSH_KIND[301] = 0;
    }
    static {
        POP_COUNT[302] = 1;
        POP_KIND[302 + 0] = 2;
        PUSH_KIND[302] = -1;
    }
    static {
        POP_COUNT[303] = 1;
        POP_KIND[303 + 0] = 2;
        PUSH_KIND[303] = -1;
    }
    static {
        POP_COUNT[304] = 1;
        POP_KIND[304 + 0] = 2;
        PUSH_KIND[304] = 0;
    }
    static {
        POP_COUNT[305] = 1;
        POP_KIND[305 + 0] = 2;
        PUSH_KIND[305] = -1;
    }
    static {
        POP_COUNT[306] = 1;
        POP_KIND[306 + 0] = 2;
        PUSH_KIND[306] = -1;
    }
    static {
        POP_COUNT[307] = 0;
        PUSH_KIND[307] = -1;
    }
    static {
        POP_COUNT[308] = 0;
        PUSH_KIND[308] = -1;
    }
    static {
        POP_COUNT[309] = 0;
        PUSH_KIND[309] = 0;
    }
    static {
        POP_COUNT[310] = 1;
        POP_KIND[310 + 0] = 0;
        PUSH_KIND[310] = -1;
    }
    static {
        POP_COUNT[311] = 1;
        POP_KIND[311 + 0] = 0;
        PUSH_KIND[311] = -1;
    }
    static {
        POP_COUNT[312] = 1;
        POP_KIND[312 + 0] = 2;
        PUSH_KIND[312] = 0;
    }
    static {
        POP_COUNT[313] = 2;
        POP_KIND[313 + 0] = 0;
        POP_KIND[313 + 1] = 2;
        PUSH_KIND[313] = 0;
    }
    static {
        POP_COUNT[314] = 2;
        POP_KIND[314 + 0] = 0;
        POP_KIND[314 + 1] = 2;
        PUSH_KIND[314] = 0;
    }
    static {
        POP_COUNT[315] = 3;
        POP_KIND[315 + 0] = 0;
        POP_KIND[315 + 1] = 1;
        POP_KIND[315 + 2] = 2;
        PUSH_KIND[315] = -1;
    }
    static {
        POP_COUNT[316] = 3;
        POP_KIND[316 + 0] = 0;
        POP_KIND[316 + 1] = 0;
        POP_KIND[316 + 2] = 2;
        PUSH_KIND[316] = -1;
    }
    static {
        POP_COUNT[317] = 3;
        POP_KIND[317 + 0] = 0;
        POP_KIND[317 + 1] = 0;
        POP_KIND[317 + 2] = 2;
        PUSH_KIND[317] = 1;
    }
    static {
        POP_COUNT[318] = 1;
        POP_KIND[318 + 0] = 1;
        PUSH_KIND[318] = 1;
    }
    static {
        POP_COUNT[319] = 1;
        POP_KIND[319 + 0] = 1;
        PUSH_KIND[319] = 1;
    }
    static {
        POP_COUNT[320] = 1;
        POP_KIND[320 + 0] = 1;
        PUSH_KIND[320] = 1;
    }
    static {
        POP_COUNT[321] = 4;
        POP_KIND[321 + 0] = 2;
        POP_KIND[321 + 1] = 0;
        POP_KIND[321 + 2] = 2;
        POP_KIND[321 + 3] = 0;
        PUSH_KIND[321] = 1;
    }
    static {
        POP_COUNT[322] = 3;
        POP_KIND[322 + 0] = 0;
        POP_KIND[322 + 1] = 0;
        POP_KIND[322 + 2] = 1;
        PUSH_KIND[322] = 1;
    }
    static {
        POP_COUNT[323] = 2;
        POP_KIND[323 + 0] = 2;
        POP_KIND[323 + 1] = 2;
        PUSH_KIND[323] = -1;
    }
    static {
        POP_COUNT[324] = 2;
        POP_KIND[324 + 0] = 0;
        POP_KIND[324 + 1] = 2;
        PUSH_KIND[324] = 0;
    }
    static {
        POP_COUNT[325] = 2;
        POP_KIND[325 + 0] = 2;
        POP_KIND[325 + 1] = 2;
        PUSH_KIND[325] = -1;
    }
    static {
        POP_COUNT[326] = 0;
        PUSH_KIND[326] = -1;
    }
    static {
        POP_COUNT[327] = 2;
        POP_KIND[327 + 0] = 2;
        POP_KIND[327 + 1] = 2;
        PUSH_KIND[327] = -1;
    }
    static {
        POP_COUNT[328] = 0;
        PUSH_KIND[328] = -1;
    }
    static {
        POP_COUNT[329] = 1;
        POP_KIND[329 + 0] = 2;
        PUSH_KIND[329] = 0;
    }
    static {
        POP_COUNT[330] = 1;
        POP_KIND[330 + 0] = 0;
        PUSH_KIND[330] = 0;
    }
    static {
        POP_COUNT[331] = 1;
        POP_KIND[331 + 0] = 0;
        PUSH_KIND[331] = 2;
    }
    static {
        POP_COUNT[332] = 2;
        POP_KIND[332 + 0] = 0;
        POP_KIND[332 + 1] = 0;
        PUSH_KIND[332] = 0;
    }
    static {
        POP_COUNT[333] = 1;
        POP_KIND[333 + 0] = 2;
        PUSH_KIND[333] = -1;
    }
    static {
        POP_COUNT[334] = 1;
        POP_KIND[334 + 0] = 0;
        PUSH_KIND[334] = -1;
    }
    static {
        POP_COUNT[335] = 0;
        PUSH_KIND[335] = 0;
    }
    static {
        POP_COUNT[336] = 1;
        POP_KIND[336 + 0] = 2;
        PUSH_KIND[336] = 0;
    }
    static {
        POP_COUNT[337] = 1;
        POP_KIND[337 + 0] = 2;
        PUSH_KIND[337] = 0;
    }
    static {
        POP_COUNT[338] = 2;
        POP_KIND[338 + 0] = 0;
        POP_KIND[338 + 1] = 2;
        PUSH_KIND[338] = -1;
    }
    static {
        POP_COUNT[339] = 0;
        PUSH_KIND[339] = -1;
    }
    static {
        POP_COUNT[340] = 1;
        POP_KIND[340 + 0] = 1;
        PUSH_KIND[340] = -1;
    }
    static {
        POP_COUNT[341] = 1;
        POP_KIND[341 + 0] = 2;
        PUSH_KIND[341] = -1;
    }
    static {
        POP_COUNT[342] = 1;
        POP_KIND[342 + 0] = 0;
        PUSH_KIND[342] = 0;
    }
    static {
        POP_COUNT[346] = 2;
        POP_KIND[346 + 0] = 0;
        POP_KIND[346 + 1] = 0;
        PUSH_KIND[346] = -1;
    }
    static {
        POP_COUNT[347] = 2;
        POP_KIND[347 + 0] = 0;
        POP_KIND[347 + 1] = 0;
        PUSH_KIND[347] = -1;
    }
    static {
        POP_COUNT[348] = 1;
        POP_KIND[348 + 0] = 0;
        PUSH_KIND[348] = 0;
    }
    static {
        POP_COUNT[349] = 1;
        POP_KIND[349 + 0] = 0;
        PUSH_KIND[349] = 0;
    }
    static {
        POP_COUNT[350] = 3;
        POP_KIND[350 + 0] = 0;
        POP_KIND[350 + 1] = 0;
        POP_KIND[350 + 2] = 2;
        PUSH_KIND[350] = -1;
    }
    static {
        POP_COUNT[351] = 3;
        POP_KIND[351 + 0] = 0;
        POP_KIND[351 + 1] = 0;
        POP_KIND[351 + 2] = 2;
        PUSH_KIND[351] = -1;
    }
    static {
        POP_COUNT[352] = 2;
        POP_KIND[352 + 0] = 0;
        POP_KIND[352 + 1] = 2;
        PUSH_KIND[352] = 0;
    }
    static {
        POP_COUNT[353] = 2;
        POP_KIND[353 + 0] = 0;
        POP_KIND[353 + 1] = 2;
        PUSH_KIND[353] = 0;
    }
    static {
        POP_COUNT[354] = 1;
        POP_KIND[354 + 0] = 0;
        PUSH_KIND[354] = -1;
    }
    static {
        POP_COUNT[355] = 0;
        PUSH_KIND[355] = 0;
    }
    static {
        POP_COUNT[356] = 0;
        PUSH_KIND[356] = 0;
    }
    static {
        POP_COUNT[362] = 1;
        POP_KIND[362 + 0] = 0;
        PUSH_KIND[362] = -1;
    }
    static {
        POP_COUNT[364] = 1;
        POP_KIND[364 + 0] = 0;
        PUSH_KIND[364] = 0;
    }
    static {
        POP_COUNT[368] = 0;
        PUSH_KIND[368] = 0;
    }
    static {
        POP_COUNT[369] = 1;
        POP_KIND[369 + 0] = 0;
        PUSH_KIND[369] = -1;
    }
    static {
        POP_COUNT[370] = 2;
        POP_KIND[370 + 0] = 0;
        POP_KIND[370 + 1] = 0;
        PUSH_KIND[370] = -1;
    }
    static {
        POP_COUNT[371] = 0;
        PUSH_KIND[371] = 0;
    }
    static {
        POP_COUNT[372] = 0;
        PUSH_KIND[372] = 0;
    }
    static {
        POP_COUNT[375] = 2;
        POP_KIND[375 + 0] = 0;
        POP_KIND[375 + 1] = 1;
        PUSH_KIND[375] = -1;
    }
    static {
        POP_COUNT[376] = 0;
        PUSH_KIND[376] = -1;
    }
    static {
        POP_COUNT[377] = 0;
        PUSH_KIND[377] = -1;
    }
    static {
        POP_COUNT[378] = 0;
        PUSH_KIND[378] = -1;
    }
    static {
        POP_COUNT[379] = 0;
        PUSH_KIND[379] = -1;
    }
    static {
        POP_COUNT[380] = 0;
        PUSH_KIND[380] = -1;
    }
    static {
        POP_COUNT[381] = 0;
        PUSH_KIND[381] = -1;
    }
    static {
        POP_COUNT[382] = 0;
        PUSH_KIND[382] = -1;
    }
    static {
        POP_COUNT[383] = 0;
        PUSH_KIND[383] = -1;
    }
    static {
        POP_COUNT[384] = 0;
        PUSH_KIND[384] = -1;
    }
    static {
        POP_COUNT[385] = 0;
        PUSH_KIND[385] = -1;
    }
    static {
        POP_COUNT[386] = 0;
        PUSH_KIND[386] = -1;
    }
    static {
        POP_COUNT[387] = 0;
        PUSH_KIND[387] = -1;
    }
    static {
        POP_COUNT[388] = 0;
        PUSH_KIND[388] = -1;
    }
    static {
        POP_COUNT[389] = 0;
        PUSH_KIND[389] = -1;
    }
    static {
        POP_COUNT[390] = 0;
        PUSH_KIND[390] = -1;
    }
    static {
        POP_COUNT[391] = 0;
        PUSH_KIND[391] = -1;
    }
    static {
        POP_COUNT[392] = 0;
        PUSH_KIND[392] = -1;
    }
    static {
        POP_COUNT[393] = 2;
        POP_KIND[393 + 0] = 1;
        POP_KIND[393 + 1] = 0;
        PUSH_KIND[393] = -1;
    }
    static {
        POP_COUNT[394] = 2;
        POP_KIND[394 + 0] = 1;
        POP_KIND[394 + 1] = 0;
        PUSH_KIND[394] = -1;
    }
    static {
        POP_COUNT[398] = 0;
        PUSH_KIND[398] = 0;
    }
    static {
        POP_COUNT[399] = 1;
        POP_KIND[399 + 0] = 0;
        PUSH_KIND[399] = -1;
    }
    static {
        POP_COUNT[401] = 1;
        POP_KIND[401 + 0] = 2;
        PUSH_KIND[401] = 0;
    }
    static {
        POP_COUNT[402] = 2;
        POP_KIND[402 + 0] = 0;
        POP_KIND[402 + 1] = 2;
        PUSH_KIND[402] = -1;
    }
    static {
        POP_COUNT[403] = 0;
        PUSH_KIND[403] = 0;
    }
    static {
        POP_COUNT[404] = 0;
        PUSH_KIND[404] = -1;
    }
    static {
        POP_COUNT[411] = 1;
        POP_KIND[411 + 0] = 0;
        PUSH_KIND[411] = -1;
    }
    static {
        POP_COUNT[413] = 2;
        POP_KIND[413 + 0] = 3;
        POP_KIND[413 + 1] = 3;
        PUSH_KIND[413] = -1;
    }
    static {
        POP_COUNT[414] = 1;
        POP_KIND[414 + 0] = 3;
        PUSH_KIND[414] = 0;
    }
    static {
        POP_COUNT[415] = 1;
        POP_KIND[415 + 0] = 3;
        PUSH_KIND[415] = -1;
    }
    static {
        POP_COUNT[418] = 1;
        POP_KIND[418 + 0] = 0;
        PUSH_KIND[418] = -1;
    }
    static {
        POP_COUNT[431] = 0;
        PUSH_KIND[431] = 0;
    }
    static {
        POP_COUNT[435] = 0;
        PUSH_KIND[435] = 0;
    }
    static {
        POP_COUNT[436] = 2;
        POP_KIND[436 + 0] = 0;
        POP_KIND[436 + 1] = 0;
        PUSH_KIND[436] = -1;
    }
    static {
        POP_COUNT[437] = 2;
        POP_KIND[437 + 0] = 0;
        POP_KIND[437 + 1] = 0;
        PUSH_KIND[437] = -1;
    }
    static {
        POP_COUNT[438] = 1;
        POP_KIND[438 + 0] = 0;
        PUSH_KIND[438] = -1;
    }
    static {
        POP_COUNT[439] = 2;
        POP_KIND[439 + 0] = 0;
        POP_KIND[439 + 1] = 0;
        PUSH_KIND[439] = -1;
    }
    static {
        POP_COUNT[440] = 2;
        POP_KIND[440 + 0] = 0;
        POP_KIND[440 + 1] = 0;
        PUSH_KIND[440] = -1;
    }
    static {
        POP_COUNT[441] = 2;
        POP_KIND[441 + 0] = 0;
        POP_KIND[441 + 1] = 0;
        PUSH_KIND[441] = -1;
    }
    static {
        POP_COUNT[442] = 2;
        POP_KIND[442 + 0] = 0;
        POP_KIND[442 + 1] = 0;
        PUSH_KIND[442] = -1;
    }
    static {
        POP_COUNT[444] = 4;
        POP_KIND[444 + 0] = 1;
        POP_KIND[444 + 1] = 0;
        POP_KIND[444 + 2] = 0;
        POP_KIND[444 + 3] = 1;
        PUSH_KIND[444] = -1;
    }
    static {
        POP_COUNT[476] = 1;
        POP_KIND[476 + 0] = 0;
        PUSH_KIND[476] = -1;
    }
    static {
        POP_COUNT[477] = 1;
        POP_KIND[477 + 0] = 0;
        PUSH_KIND[477] = -1;
    }
    static {
        POP_COUNT[478] = 1;
        POP_KIND[478 + 0] = 0;
        PUSH_KIND[478] = 0;
    }
    static {
        POP_COUNT[479] = 1;
        POP_KIND[479 + 0] = 0;
        PUSH_KIND[479] = 0;
    }
    static {
        POP_COUNT[480] = 2;
        POP_KIND[480 + 0] = 0;
        POP_KIND[480 + 1] = 0;
        PUSH_KIND[480] = -1;
    }
    static {
        POP_COUNT[481] = 5;
        POP_KIND[481 + 0] = 0;
        POP_KIND[481 + 1] = 0;
        POP_KIND[481 + 2] = 0;
        POP_KIND[481 + 3] = 0;
        POP_KIND[481 + 4] = 0;
        PUSH_KIND[481] = -1;
    }
    static {
        POP_COUNT[482] = 4;
        POP_KIND[482 + 0] = 0;
        POP_KIND[482 + 1] = 0;
        POP_KIND[482 + 2] = 0;
        POP_KIND[482 + 3] = 0;
        PUSH_KIND[482] = 0;
    }
    static {
        POP_COUNT[483] = 4;
        POP_KIND[483 + 0] = 0;
        POP_KIND[483 + 1] = 0;
        POP_KIND[483 + 2] = 0;
        POP_KIND[483 + 3] = 0;
        PUSH_KIND[483] = -1;
    }
    static {
        POP_COUNT[486] = 1;
        POP_KIND[486 + 0] = 0;
        PUSH_KIND[486] = -1;
    }
    static {
        POP_COUNT[487] = 0;
        PUSH_KIND[487] = 0;
    }
    static {
        POP_COUNT[488] = 1;
        POP_KIND[488 + 0] = 0;
        PUSH_KIND[488] = -1;
    }
    static {
        POP_COUNT[489] = 0;
        PUSH_KIND[489] = 0;
    }
    static {
        POP_COUNT[492] = 1;
        POP_KIND[492 + 0] = 3;
        PUSH_KIND[492] = -1;
    }
    static {
        POP_COUNT[493] = 1;
        POP_KIND[493 + 0] = 3;
        PUSH_KIND[493] = 0;
    }
    static {
        POP_COUNT[494] = 1;
        POP_KIND[494 + 0] = 3;
        PUSH_KIND[494] = -1;
    }
    static {
        POP_COUNT[495] = 1;
        POP_KIND[495 + 0] = 3;
        PUSH_KIND[495] = -1;
    }
    static {
        POP_COUNT[496] = 1;
        POP_KIND[496 + 0] = 3;
        PUSH_KIND[496] = -1;
    }
    static {
        POP_COUNT[497] = 2;
        POP_KIND[497 + 0] = 3;
        POP_KIND[497 + 1] = 3;
        PUSH_KIND[497] = -1;
    }
    static {
        POP_COUNT[499] = 1;
        POP_KIND[499 + 0] = 2;
        PUSH_KIND[499] = -1;
    }
    static {
        POP_COUNT[500] = 2;
        POP_KIND[500 + 0] = 0;
        POP_KIND[500 + 1] = 2;
        PUSH_KIND[500] = -1;
    }
    static {
        POP_COUNT[501] = 1;
        POP_KIND[501 + 0] = 2;
        PUSH_KIND[501] = 0;
    }
    static {
        POP_COUNT[516] = 2;
        POP_KIND[516 + 0] = 0;
        POP_KIND[516 + 1] = 0;
        PUSH_KIND[516] = 0;
    }
    static {
        POP_COUNT[517] = 3;
        POP_KIND[517 + 0] = 0;
        POP_KIND[517 + 1] = 0;
        POP_KIND[517 + 2] = 0;
        PUSH_KIND[517] = -1;
    }
    static {
        POP_COUNT[518] = 1;
        POP_KIND[518 + 0] = 2;
        PUSH_KIND[518] = -1;
    }
    static {
        POP_COUNT[519] = 1;
        POP_KIND[519 + 0] = 0;
        PUSH_KIND[519] = -1;
    }
    static {
        POP_COUNT[525] = 1;
        POP_KIND[525 + 0] = 0;
        PUSH_KIND[525] = 0;
    }
    static {
        POP_COUNT[526] = 1;
        POP_KIND[526 + 0] = 0;
        PUSH_KIND[526] = 2;
    }
    static {
        POP_COUNT[527] = 1;
        POP_KIND[527 + 0] = 0;
        PUSH_KIND[527] = -1;
    }
    static {
        POP_COUNT[528] = 0;
        PUSH_KIND[528] = 0;
    }
    static {
        POP_COUNT[529] = 0;
        PUSH_KIND[529] = 0;
    }
    static {
        POP_COUNT[530] = 0;
        PUSH_KIND[530] = 0;
    }
    static {
        POP_COUNT[532] = 1;
        POP_KIND[532 + 0] = 0;
        PUSH_KIND[532] = -1;
    }
    static {
        POP_COUNT[533] = 1;
        POP_KIND[533 + 0] = 0;
        PUSH_KIND[533] = -1;
    }
    static {
        POP_COUNT[534] = 2;
        POP_KIND[534 + 0] = 0;
        POP_KIND[534 + 1] = 2;
        PUSH_KIND[534] = -1;
    }
    static {
        POP_COUNT[535] = 1;
        POP_KIND[535 + 0] = 2;
        PUSH_KIND[535] = 0;
    }
    static {
        POP_COUNT[536] = 2;
        POP_KIND[536 + 0] = 0;
        POP_KIND[536 + 1] = 2;
        PUSH_KIND[536] = -1;
    }
    static {
        POP_COUNT[537] = 1;
        POP_KIND[537 + 0] = 2;
        PUSH_KIND[537] = 0;
    }
    static {
        POP_COUNT[538] = 2;
        POP_KIND[538 + 0] = 0;
        POP_KIND[538 + 1] = 2;
        PUSH_KIND[538] = -1;
    }
    static {
        POP_COUNT[540] = 0;
        PUSH_KIND[540] = 0;
    }
    static {
        POP_COUNT[541] = 0;
        PUSH_KIND[541] = 0;
    }
    static {
        POP_COUNT[542] = 0;
        PUSH_KIND[542] = 0;
    }
    static {
        POP_COUNT[543] = 0;
        PUSH_KIND[543] = 0;
    }
    static {
        POP_COUNT[544] = 0;
        PUSH_KIND[544] = 0;
    }
    static {
        POP_COUNT[545] = 0;
        PUSH_KIND[545] = 0;
    }
    static {
        POP_COUNT[548] = 0;
        PUSH_KIND[548] = -1;
    }
    static {
        POP_COUNT[550] = 0;
        PUSH_KIND[550] = 0;
    }
    static {
        POP_COUNT[551] = 0;
        PUSH_KIND[551] = -1;
    }
    static {
        POP_COUNT[552] = 0;
        PUSH_KIND[552] = 0;
    }
    static {
        POP_COUNT[553] = 2;
        POP_KIND[553 + 0] = 0;
        POP_KIND[553 + 1] = 0;
        PUSH_KIND[553] = -1;
    }
    static {
        POP_COUNT[554] = 1;
        POP_KIND[554 + 0] = 3;
        PUSH_KIND[554] = -1;
    }
    static {
        POP_COUNT[555] = 2;
        POP_KIND[555 + 0] = 0;
        POP_KIND[555 + 1] = 1;
        PUSH_KIND[555] = 0;
    }
    static {
        POP_COUNT[556] = 1;
        POP_KIND[556 + 0] = 0;
        PUSH_KIND[556] = -1;
    }
    static {
        POP_COUNT[557] = 2;
        POP_KIND[557 + 0] = 0;
        POP_KIND[557 + 1] = 0;
        PUSH_KIND[557] = 0;
    }
    static {
        POP_COUNT[558] = 3;
        POP_KIND[558 + 0] = 3;
        POP_KIND[558 + 1] = 3;
        POP_KIND[558 + 2] = 0;
        PUSH_KIND[558] = -1;
    }
    static {
        POP_COUNT[559] = 2;
        POP_KIND[559 + 0] = 3;
        POP_KIND[559 + 1] = 3;
        PUSH_KIND[559] = 3;
    }
    static {
        POP_COUNT[560] = 1;
        POP_KIND[560 + 0] = 0;
        PUSH_KIND[560] = -1;
    }
    static {
        POP_COUNT[561] = 1;
        POP_KIND[561 + 0] = 0;
        PUSH_KIND[561] = 0;
    }
    static {
        POP_COUNT[562] = 2;
        POP_KIND[562 + 0] = 0;
        POP_KIND[562 + 1] = 0;
        PUSH_KIND[562] = -1;
    }
    static {
        POP_COUNT[563] = 2;
        POP_KIND[563 + 0] = 0;
        POP_KIND[563 + 1] = 0;
        PUSH_KIND[563] = 0;
    }
    static {
        POP_COUNT[564] = 1;
        POP_KIND[564 + 0] = 0;
        PUSH_KIND[564] = -1;
    }
    static {
        POP_COUNT[565] = 2;
        POP_KIND[565 + 0] = 1;
        POP_KIND[565 + 1] = 1;
        PUSH_KIND[565] = 0;
    }
    static {
        POP_COUNT[566] = 1;
        POP_KIND[566 + 0] = 0;
        PUSH_KIND[566] = 0;
    }
    static {
        POP_COUNT[567] = 1;
        POP_KIND[567 + 0] = 1;
        PUSH_KIND[567] = 0;
    }
    static {
        POP_COUNT[568] = 1;
        POP_KIND[568 + 0] = 1;
        PUSH_KIND[568] = -1;
    }
    static {
        POP_COUNT[569] = 2;
        POP_KIND[569 + 0] = 3;
        POP_KIND[569 + 1] = 0;
        PUSH_KIND[569] = 3;
    }
    static {
        POP_COUNT[570] = 1;
        POP_KIND[570 + 0] = 0;
        PUSH_KIND[570] = 0;
    }
    static {
        POP_COUNT[574] = 1;
        POP_KIND[574 + 0] = 0;
        PUSH_KIND[574] = -1;
    }
    static {
        POP_COUNT[575] = 1;
        POP_KIND[575 + 0] = 0;
        PUSH_KIND[575] = -1;
    }
    static {
        POP_COUNT[576] = 1;
        POP_KIND[576 + 0] = 0;
        PUSH_KIND[576] = -1;
    }
    static {
        POP_COUNT[578] = 3;
        POP_KIND[578 + 0] = 0;
        POP_KIND[578 + 1] = 0;
        POP_KIND[578 + 2] = 2;
        PUSH_KIND[578] = -1;
    }
    static {
        POP_COUNT[579] = 2;
        POP_KIND[579 + 0] = 0;
        POP_KIND[579 + 1] = 2;
        PUSH_KIND[579] = 0;
    }
    static {
        POP_COUNT[580] = 1;
        POP_KIND[580 + 0] = 0;
        PUSH_KIND[580] = -1;
    }
    static {
        POP_COUNT[581] = 0;
        PUSH_KIND[581] = 0;
    }
    static {
        POP_COUNT[582] = 1;
        POP_KIND[582 + 0] = 0;
        PUSH_KIND[582] = -1;
    }
    static {
        POP_COUNT[583] = 1;
        POP_KIND[583 + 0] = 0;
        PUSH_KIND[583] = -1;
    }
    static {
        POP_COUNT[587] = 0;
        PUSH_KIND[587] = 0;
    }
    static {
        POP_COUNT[588] = 0;
        PUSH_KIND[588] = 0;
    }
    static {
        POP_COUNT[589] = 1;
        POP_KIND[589 + 0] = 0;
        PUSH_KIND[589] = -1;
    }
    static {
        POP_COUNT[590] = 3;
        POP_KIND[590 + 0] = 0;
        POP_KIND[590 + 1] = 0;
        POP_KIND[590 + 2] = 1;
        PUSH_KIND[590] = 1;
    }
    static {
        POP_COUNT[591] = 1;
        POP_KIND[591 + 0] = 1;
        PUSH_KIND[591] = 0;
    }
    static {
        POP_COUNT[592] = 2;
        POP_KIND[592 + 0] = 3;
        POP_KIND[592 + 1] = 3;
        PUSH_KIND[592] = -1;
    }
    static {
        POP_COUNT[593] = 1;
        POP_KIND[593 + 0] = 1;
        PUSH_KIND[593] = 0;
    }
    static {
        POP_COUNT[595] = 1;
        POP_KIND[595 + 0] = 3;
        PUSH_KIND[595] = 0;
    }
    static {
        POP_COUNT[596] = 2;
        POP_KIND[596 + 0] = 0;
        POP_KIND[596 + 1] = 3;
        PUSH_KIND[596] = -1;
    }
    static {
        POP_COUNT[597] = 1;
        POP_KIND[597 + 0] = 3;
        PUSH_KIND[597] = 0;
    }
    static {
        POP_COUNT[598] = 2;
        POP_KIND[598 + 0] = 0;
        POP_KIND[598 + 1] = 0;
        PUSH_KIND[598] = 3;
    }
    static {
        POP_COUNT[599] = 2;
        POP_KIND[599 + 0] = 3;
        POP_KIND[599 + 1] = 3;
        PUSH_KIND[599] = 0;
    }
    static {
        POP_COUNT[609] = 3;
        POP_KIND[609 + 0] = 0;
        POP_KIND[609 + 1] = 0;
        POP_KIND[609 + 2] = 0;
        PUSH_KIND[609] = 0;
    }
    static {
        POP_COUNT[610] = 2;
        POP_KIND[610 + 0] = 0;
        POP_KIND[610 + 1] = 0;
        PUSH_KIND[610] = -1;
    }
    static {
        POP_COUNT[611] = 2;
        POP_KIND[611 + 0] = 3;
        POP_KIND[611 + 1] = 3;
        PUSH_KIND[611] = 0;
    }
    static {
        POP_COUNT[612] = 1;
        POP_KIND[612 + 0] = 3;
        PUSH_KIND[612] = -1;
    }
    static {
        POP_COUNT[613] = 1;
        POP_KIND[613 + 0] = 3;
        PUSH_KIND[613] = -1;
    }
    static {
        POP_COUNT[614] = 1;
        POP_KIND[614 + 0] = 3;
        PUSH_KIND[614] = 0;
    }
    static {
        POP_COUNT[615] = 1;
        POP_KIND[615 + 0] = 3;
        PUSH_KIND[615] = 0;
    }
    static {
        POP_COUNT[619] = 2;
        POP_KIND[619 + 0] = 0;
        POP_KIND[619 + 1] = 0;
        PUSH_KIND[619] = 1;
    }
    static {
        POP_COUNT[620] = 0;
        PUSH_KIND[620] = 0;
    }
    static {
        POP_COUNT[621] = 2;
        POP_KIND[621 + 0] = 0;
        POP_KIND[621 + 1] = 0;
        PUSH_KIND[621] = 0;
    }
    static {
        POP_COUNT[622] = 3;
        POP_KIND[622 + 0] = 0;
        POP_KIND[622 + 1] = 0;
        POP_KIND[622 + 2] = 2;
        PUSH_KIND[622] = 2;
    }
    static {
        POP_COUNT[623] = 3;
        POP_KIND[623 + 0] = 0;
        POP_KIND[623 + 1] = 0;
        POP_KIND[623 + 2] = 0;
        PUSH_KIND[623] = 2;
    }
    static {
        POP_COUNT[624] = 2;
        POP_KIND[624 + 0] = 0;
        POP_KIND[624 + 1] = 0;
        PUSH_KIND[624] = 0;
    }
    static {
        POP_COUNT[625] = 1;
        POP_KIND[625 + 0] = 0;
        PUSH_KIND[625] = 0;
    }
    static {
        POP_COUNT[626] = 3;
        POP_KIND[626 + 0] = 0;
        POP_KIND[626 + 1] = 0;
        POP_KIND[626 + 2] = 2;
        PUSH_KIND[626] = 0;
    }
    static {
        POP_COUNT[627] = 4;
        POP_KIND[627 + 0] = 0;
        POP_KIND[627 + 1] = 0;
        POP_KIND[627 + 2] = 0;
        POP_KIND[627 + 3] = 0;
        PUSH_KIND[627] = 2;
    }
    static {
        POP_COUNT[628] = 1;
        POP_KIND[628 + 0] = 0;
        PUSH_KIND[628] = 0;
    }
    static {
        POP_COUNT[629] = 2;
        POP_KIND[629 + 0] = 2;
        POP_KIND[629 + 1] = 2;
        PUSH_KIND[629] = 0;
    }
    static {
        POP_COUNT[630] = 0;
        PUSH_KIND[630] = -1;
    }
    static {
        POP_COUNT[631] = 0;
        PUSH_KIND[631] = -1;
    }
    static {
        POP_COUNT[632] = 0;
        PUSH_KIND[632] = -1;
    }
    static {
        POP_COUNT[633] = 0;
        PUSH_KIND[633] = -1;
    }
    static {
        POP_COUNT[634] = 0;
        PUSH_KIND[634] = -1;
    }
    static {
        POP_COUNT[635] = 0;
        PUSH_KIND[635] = -1;
    }
    static {
        POP_COUNT[636] = 0;
        PUSH_KIND[636] = -1;
    }
    static {
        POP_COUNT[639] = 2;
        POP_KIND[639 + 0] = 3;
        POP_KIND[639 + 1] = 3;
        PUSH_KIND[639] = 0;
    }
    static {
        POP_COUNT[640] = 0;
        PUSH_KIND[640] = -1;
    }
    static {
        POP_COUNT[641] = 0;
        PUSH_KIND[641] = -1;
    }

    private ScriptOpcodeShapes() {
    }
}
