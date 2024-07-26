// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1

import com.ericsson.mts.asn1.factory.AbstractTranslatorFactory
import com.ericsson.mts.asn1.translator.*

class PERTranslatorFactoryKT : AbstractTranslatorFactory {
    private var perTranscoder: PERTranscoderKT

    constructor(aligned: Boolean) {
        perTranscoder = PERTranscoderKT(aligned)
    }

    constructor(perTranscoder: PERTranscoderKT) {
        this.perTranscoder = perTranscoder
    }

    override fun bitStringTranslator(): AbstractBitStringTranslator {
        return PERBitStringTranslatorKT(perTranscoder)
    }

    override fun booleanTranslator(): AbstractBooleanTranslator {
        return PERBooleanTranslatorKT()
    }

    override fun choiceTranslator(): AbstractChoiceTranslator {
        return PERChoiceTranslatorKT(perTranscoder)
    }

    override fun enumeratedTranslator(): AbstractEnumeratedTranslator {
        return PEREnumeratedTranslatorKT(perTranscoder)
    }

    override fun integerTranslator(): AbstractIntegerTranslator {
        return PERIntegerTranslatorKT(perTranscoder)
    }

    override fun octetStringTranslator(): AbstractOctetStringTranslator {
        return PEROctetStringTranslatorKT(perTranscoder)
    }

    override fun realTranslator(): AbstractRealTranslator {
        return PERRealTranslatorKT(perTranscoder)
    }

    override fun characterStringTranslator(): AbstractRestrictedCharacterStringTranslator {
        return PERCharacterStringTranslatorKT(perTranscoder)
    }

    override fun sequenceOfTranslator(): AbstractSequenceOfTranslator {
        return PERSequenceOfTranslatorKT(perTranscoder)
    }

    override fun sequenceTranslator(): AbstractSequenceTranslator {
        return PERSequenceTranslatorKT(perTranscoder)
    }

    override fun objectClassFieldTypeTranslator(): AbstractObjectClassFieldTranslator {
        return PERObjectClassFieldTranslatorKT(perTranscoder)
    }

    override fun objectIdentifierTranslator(): AbstractObjectIdentifierTranslator {
        return PERObjectIdentifierTranslatorKT(perTranscoder)
    }
}
