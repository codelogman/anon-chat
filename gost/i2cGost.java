package com.node.gost;

import com.node.gost.GostEvil;

/**
 * Created by Alex_Strange
 */
public class i2cGost {
    private final int[] key;

    private static final int[][] S_CONST = {
            {0x04,0x0a,0x09,0x02,0x0d,0x08,0x00,0x0e,0x06,0x0B,0x01,0x0c,0x07,0x0f,0x05,0x03},
            {0x0e,0x0b,0x04,0x0c,0x06,0x0d,0x0f,0x0a,0x02,0x03,0x08,0x01,0x00,0x07,0x05,0x09},
            {0x05,0x08,0x01,0x0d,0x0a,0x03,0x04,0x02,0x0e,0x0f,0x0c,0x07,0x06,0x00,0x09,0x0b},
            {0x07,0x0d,0x0a,0x01,0x00,0x08,0x09,0x0f,0x0e,0x04,0x06,0x0c,0x0b,0x02,0x05,0x03},
            {0x06,0x0c,0x07,0x01,0x05,0x0f,0x0d,0x08,0x04,0x0a,0x09,0x0e,0x00,0x03,0x0b,0x02},
            {0x04,0x0b,0x0a,0x00,0x07,0x02,0x01,0x0d,0x03,0x06,0x08,0x05,0x09,0x0c,0x0f,0x0e},
            {0x0d,0x0b,0x04,0x01,0x03,0x0f,0x05,0x09,0x00,0x0a,0x0e,0x07,0x06,0x08,0x02,0x0c},
            {0x01,0x0f,0x0d,0x00,0x05,0x07,0x0a,0x04,0x09,0x02,0x03,0x0e,0x06,0x0b,0x08,0x0c}
    };

    public i2cGost(int[] key){
        this.key = key;
    }

    public i2cGost(String key){
        GostEvil gostEvil = new GostEvil(256);
        this.key =  gostEvil.getHash(getIntFromString(key));
    }

    private boolean[] arrayIntToBitArray(int[] array){
        int sz = array.length;
        boolean[] result = new boolean[sz*4];
        for(int i = 0 ; i<sz*4; i++){
            result[i] = false;
        }
        for(int j = 0; j<sz; j++){
            for(int i = 0; i<4; i++){
                boolean flag = (array[j] & (1<<i)) != 0;
                result[j*4+i] = flag;
            }
        }
        return result;
    }

    //matriz de inversión para booleano
    private boolean[] arrayReverseBoolean(boolean[] array){
        int sz = array.length;
        boolean[] result = new boolean[sz];
        boolean temp;
        for(int i=0; i<sz/2; i++){
            temp = array[sz-i-1];
            result[sz-i-1] = array[i];
            result[i] = temp;
        }
        return result;
    }

    //inversión de matriz para int
    private int[] arrayReverseInt(int[] array){
        int sz = array.length;
        int[] result = new int[sz];
        int temp;
        for(int i=0; i<=sz/2; i++){
            temp = array[sz-i-1];
            result[sz-i-1] = array[i];
            result[i] = temp;
        }
        return result;
    }

    //Además módulo 2
    private int xor2(int k, int a){
        return k^a;
    }

    private int[] getIntFromString(String str){
        int[] result = new int[str.length()];
        for(int i=0; i<str.length(); i++){
            result[i] = (int) str.charAt(i);
        }
        return result;
    }

    //sustitución en el bloque s
    private int[] sBlock(int[] input){
        int[] result = new int[input.length];
        for(int i=input.length-1; i>=0; i--){
            result[i] = S_CONST[i][input[i]];
        }
        return result;
    }

    private int addByMod32(int n1, int x) {
        return n1+x;
    }

    private int arrayBitToIntValue(boolean[] tempBit) {
        int result = 0x00000000;
        int flag;
        for(int i = 0; i<tempBit.length; i++){
            flag = tempBit[i]?1:0;
            result = result | (flag << i);
        }
        return result;
    }

    // cambiar en la dirección de los dígitos más altos en << 11
    private boolean[] rShift(boolean[] input){
        boolean[] result = input;
        for(int i=0; i<11; i++){
            boolean tempFlag = result[result.length-1];
            for(int j = result.length-1; j>0; j--){
                result[j] = result[j-1];
            }
            result[0] = tempFlag;
        }
        return result;
    }

    //el paso principal de la criptoconversión
    private int[] mainStep(int[] n, int x){
        int[] result = new int[2];
        int n1 = n[0];
        int n2 = n[1];
        n1 = addByMod32(n1, x);
        int[] temp = new int[8];
        int i = 0;
        int s = 0;
        while(i<8){
            temp[i] = (n1 & (0xF << s))>>>s;
            s+=4;
            i++;
        }
        temp = sBlock(temp);
        boolean[] tempBit = arrayIntToBitArray(temp);
        tempBit = rShift(tempBit);
        n1 = arrayBitToIntValue(tempBit);
        n1 = xor2(n1, n2);
        result[0] = n1;
        result[1] = n[0];
        return result;
    }


    private int[] crypt32(int[] block){
        for(int j = 0; j<3; j++){
            for(int i = 0 ; i < 8; i++){
                block = mainStep(block, key[i]);
            }
        }
        for(int i = 7; i >= 0; i--){
            block = mainStep(block, key[i]);
        }
        int t = block[0];
        block[0] = block[1];
        block[1] = t;
        return block;
    }

    private int[] decrypt32(int[] block){
        for(int i = 0; i <8; i++){
            block = mainStep(block, key[i]);
        }
        for(int j = 0; j<3; j++){
            for(int i = 7 ; i >=0; i--){
                block = mainStep(block, key[i]);
            }
        }
        int t = block[0];
        block[0] = block[1];
        block[1] = t;
        return block;
    }

    private int[] mac16(int[] block){
        for(int k=1; k<=2; k++){
            for(int i = 0; i<8; i++){
                block = mainStep(block, key[i]);
            }
        }
        return block;
    }

    public int[] ecbMode(int[] block, int mode){

        if(mode == 0){
            int[] newBlock = new int[2];

            int[] result = new int[block.length];

            newBlock[0] = 0;
            newBlock[1] = 0;

            for(int i=0; i<block.length; i++){
                newBlock[0] = block[i];
                try{newBlock[1] = block[i+1];}catch(ArrayIndexOutOfBoundsException ex){
                    newBlock[1] = 0;
                }

                newBlock = crypt32(newBlock);
                result[i] = newBlock[0];
                try{result[i+1] = newBlock[1];}catch(ArrayIndexOutOfBoundsException ex){

                }
                i++;
            }
            return result;
        }else{
            int[] newBlock = new int[2];

            int[] result = new int[block.length];

            newBlock[0] = 0;
            newBlock[1] = 0;

            for(int i=0; i<block.length; i++){
                newBlock[0] = block[i];
                try{newBlock[1] = block[i+1];}catch(ArrayIndexOutOfBoundsException ex){
                    newBlock[1] = 0;
                }

                newBlock = decrypt32(newBlock);
                result[i] = newBlock[0];
                try{result[i+1] = newBlock[1];}catch(ArrayIndexOutOfBoundsException ex){

                }
                i++;
            }
            return result;
        }
    }

    public int[] cbcMode(int[] block, int[] s){

        //para un bloque de cualquier longitud para hacer
        s = crypt32(s);
        int c1 = 0x1010101;
        int c2 = 0x1010104;
        s[0] = (s[0] + c1) & 0xFFFFFFFF;
        if(s[1] > 0xFFFFFFFF - c2) s[1]++;
        s[1] = (s[1] + c2) & 0xFFFFFFFF;
        int[] gamma = crypt32(s);
        block[0] = block[0] ^ gamma[0];
        block[1] = block[1] ^ gamma[1];
        return block;
    }

    public int[] cfbMode(int[] block, int[] s){

        int[] newBlock = new int[2];

        int[] result = new int[block.length];

        for(int i=0; i<block.length; i++){
            newBlock[0] = block[i];
            try{newBlock[1] = block[i+1];}catch(ArrayIndexOutOfBoundsException ex){

            }
            int[] gamma = crypt32(s);
            newBlock[0] = newBlock[0] ^ gamma[0];
            newBlock[1] = newBlock[1] ^ gamma[1];
            s = gamma;
            result[i] = newBlock[0];
            try{result[i+1] = newBlock[1];}catch(ArrayIndexOutOfBoundsException ex){

            }
            i++;
        }
        return result;
    }

    public int[] macMode(int[] block){
        //para un bloque de cualquier longitud para hacer
        int[] s = {0,0};
        int[] sBlock = {s[0]^block[0], s[1]^block[1]};
        s = mac16(sBlock);
        return s;
    }

}
