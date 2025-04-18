package hirehive.address.logic.parser;

import static hirehive.address.logic.parser.CliSyntax.PREFIX_TAG;

import org.junit.jupiter.api.Test;

import hirehive.address.commons.core.index.Index;
import hirehive.address.commons.util.StringUtil;
import hirehive.address.logic.Messages;
import hirehive.address.logic.commands.CommandTestUtil;
import hirehive.address.logic.commands.EditCommand;
import hirehive.address.model.person.Address;
import hirehive.address.model.person.Email;
import hirehive.address.model.person.InterviewDate;
import hirehive.address.model.person.Name;
import hirehive.address.model.person.Phone;
import hirehive.address.model.tag.Tag;
import hirehive.address.testutil.EditPersonDescriptorBuilder;
import hirehive.address.testutil.TypicalIndexes;

public class EditCommandParserTest {

    private static final String TAG_EMPTY = " " + PREFIX_TAG;

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE);

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_missingParts_failure() {
        // no index specified
        CommandParserTestUtil.assertParseFailure(parser, CommandTestUtil.VALID_NAME_AMY, ParserUtil.MESSAGE_INVALID_INDEX);

        // no field specified
        CommandParserTestUtil.assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED);

        // no index and no field specified
        CommandParserTestUtil.assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidPreamble_failure() {
        // negative index
        CommandParserTestUtil.assertParseFailure(parser, "-5" + CommandTestUtil.NAME_DESC_AMY, ParserUtil.MESSAGE_INVALID_INDEX);

        // zero index
        CommandParserTestUtil.assertParseFailure(parser, "0" + CommandTestUtil.NAME_DESC_AMY, ParserUtil.MESSAGE_INVALID_INDEX);

        // invalid arguments being parsed as preamble
        CommandParserTestUtil.assertParseFailure(parser, "1 some random string", ParserUtil.MESSAGE_INVALID_INDEX);

        // invalid prefix being parsed as preamble
        CommandParserTestUtil.assertParseFailure(parser, "1 z/ string", ParserUtil.MESSAGE_INVALID_INDEX);

        // out of range integer
        CommandParserTestUtil.assertParseFailure(parser, "100000000000000000000000000000 n/Alice", ParserUtil.MESSAGE_OUT_OF_RANGE);
    }

    @Test
    public void parse_invalidValue_failure() {
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_NAME_DESC, Name.MESSAGE_CONSTRAINTS); // invalid name
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_PHONE_DESC, Phone.MESSAGE_CONSTRAINTS); // invalid phone
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_EMAIL_DESC, Email.MESSAGE_CONSTRAINTS); // invalid email
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_ADDRESS_DESC, Address.MESSAGE_CONSTRAINTS); // invalid address
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_TAG_DESC, Tag.MESSAGE_CONSTRAINTS); // invalid tag
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_DATE_DESC, InterviewDate.MESSAGE_CONSTRAINTS); // invalid date

        // invalid phone followed by valid email
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_PHONE_DESC + CommandTestUtil.EMAIL_DESC_AMY, Phone.MESSAGE_CONSTRAINTS);

        // while parsing {@code PREFIX_TAG} alone will reset the tags of the {@code Person} being edited,
        // parsing it together with a valid tag results in error
        CommandParserTestUtil.assertParseFailure(parser,
                "1" + CommandTestUtil.TAG_DESC_APPLICANT + CommandTestUtil.TAG_DESC_CANDIDATE + TAG_EMPTY,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG));
        CommandParserTestUtil.assertParseFailure(parser,
                "1" + CommandTestUtil.TAG_DESC_APPLICANT + TAG_EMPTY + CommandTestUtil.TAG_DESC_CANDIDATE,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG));
        CommandParserTestUtil.assertParseFailure(parser,
                "1" + TAG_EMPTY + CommandTestUtil.TAG_DESC_APPLICANT + CommandTestUtil.TAG_DESC_CANDIDATE,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_TAG));

        // multiple invalid values, but only the first invalid value is captured
        CommandParserTestUtil.assertParseFailure(parser, "1" + CommandTestUtil.INVALID_NAME_DESC + CommandTestUtil.INVALID_EMAIL_DESC + CommandTestUtil.VALID_ADDRESS_AMY + CommandTestUtil.VALID_PHONE_AMY,
                Name.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_allFieldsSpecified_success() {
        Index targetIndex = TypicalIndexes.INDEX_SECOND_PERSON;
        String userInput = targetIndex.getOneBased() + CommandTestUtil.PHONE_DESC_BOB + CommandTestUtil.EMAIL_DESC_AMY
                + CommandTestUtil.ADDRESS_DESC_AMY + CommandTestUtil.NAME_DESC_AMY + CommandTestUtil.TAG_DESC_APPLICANT;

        EditCommand.EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(CommandTestUtil.VALID_NAME_AMY)
                .withPhone(CommandTestUtil.VALID_PHONE_BOB).withEmail(CommandTestUtil.VALID_EMAIL_AMY).withAddress(CommandTestUtil.VALID_ADDRESS_AMY)
                .withTag(CommandTestUtil.VALID_TAG_APPLICANT).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_someFieldsSpecified_success() {
        Index targetIndex = TypicalIndexes.INDEX_FIRST_PERSON;
        String userInput = targetIndex.getOneBased() + CommandTestUtil.PHONE_DESC_BOB + CommandTestUtil.EMAIL_DESC_AMY;

        EditCommand.EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withPhone(CommandTestUtil.VALID_PHONE_BOB)
                .withEmail(CommandTestUtil.VALID_EMAIL_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_oneFieldSpecified_success() {
        // name
        Index targetIndex = TypicalIndexes.INDEX_THIRD_PERSON;
        String userInput = targetIndex.getOneBased() + CommandTestUtil.NAME_DESC_AMY;
        EditCommand.EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName(CommandTestUtil.VALID_NAME_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // phone
        userInput = targetIndex.getOneBased() + CommandTestUtil.PHONE_DESC_AMY;
        descriptor = new EditPersonDescriptorBuilder().withPhone(CommandTestUtil.VALID_PHONE_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // email
        userInput = targetIndex.getOneBased() + CommandTestUtil.EMAIL_DESC_AMY;
        descriptor = new EditPersonDescriptorBuilder().withEmail(CommandTestUtil.VALID_EMAIL_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // address
        userInput = targetIndex.getOneBased() + CommandTestUtil.ADDRESS_DESC_AMY;
        descriptor = new EditPersonDescriptorBuilder().withAddress(CommandTestUtil.VALID_ADDRESS_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // tags
        userInput = targetIndex.getOneBased() + CommandTestUtil.TAG_DESC_APPLICANT;
        descriptor = new EditPersonDescriptorBuilder().withTag(CommandTestUtil.VALID_TAG_APPLICANT).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);

        // dates
        userInput = targetIndex.getOneBased() + CommandTestUtil.DATE_DESC_BOB;
        descriptor = new EditPersonDescriptorBuilder().withDate(CommandTestUtil.VALID_DATE_BOB).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        CommandParserTestUtil.assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_multipleRepeatedFields_failure() {
        // More extensive testing of duplicate parameter detections is done in
        // AddCommandParserTest#parse_repeatedNonTagValue_failure()

        // valid followed by invalid
        Index targetIndex = TypicalIndexes.INDEX_FIRST_PERSON;
        String userInput = targetIndex.getOneBased() + CommandTestUtil.INVALID_PHONE_DESC + CommandTestUtil.PHONE_DESC_BOB;

        CommandParserTestUtil.assertParseFailure(parser, userInput, Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_PHONE));

        // invalid followed by valid
        userInput = targetIndex.getOneBased() + CommandTestUtil.PHONE_DESC_BOB + CommandTestUtil.INVALID_PHONE_DESC;

        CommandParserTestUtil.assertParseFailure(parser, userInput, Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_PHONE));

        // mulltiple valid fields repeated
        userInput = targetIndex.getOneBased() + CommandTestUtil.PHONE_DESC_AMY + CommandTestUtil.ADDRESS_DESC_AMY + CommandTestUtil.EMAIL_DESC_AMY
                + CommandTestUtil.TAG_DESC_APPLICANT + CommandTestUtil.PHONE_DESC_AMY + CommandTestUtil.ADDRESS_DESC_AMY + CommandTestUtil.EMAIL_DESC_AMY + CommandTestUtil.TAG_DESC_APPLICANT
                + CommandTestUtil.PHONE_DESC_BOB + CommandTestUtil.ADDRESS_DESC_BOB + CommandTestUtil.EMAIL_DESC_BOB + CommandTestUtil.TAG_DESC_CANDIDATE;

        CommandParserTestUtil.assertParseFailure(parser, userInput,
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_PHONE, CliSyntax.PREFIX_EMAIL,
                        CliSyntax.PREFIX_ADDRESS, PREFIX_TAG));

        // multiple invalid values
        userInput = targetIndex.getOneBased() + CommandTestUtil.INVALID_PHONE_DESC + CommandTestUtil.INVALID_ADDRESS_DESC + CommandTestUtil.INVALID_EMAIL_DESC
                + CommandTestUtil.INVALID_PHONE_DESC + CommandTestUtil.INVALID_ADDRESS_DESC + CommandTestUtil.INVALID_EMAIL_DESC;

        CommandParserTestUtil.assertParseFailure(parser, userInput,
                Messages.getErrorMessageForDuplicatePrefixes(CliSyntax.PREFIX_PHONE, CliSyntax.PREFIX_EMAIL, CliSyntax.PREFIX_ADDRESS));
    }
}
