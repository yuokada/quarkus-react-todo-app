import PropTypes from 'prop-types';

function MyButton({count, onClick}) {
  return (
      <button onClick={onClick}>Clicked {count} times</button>
  );
}

MyButton.propTypes = {
  count: PropTypes.number.isRequired,
  onClick: PropTypes.func.isRequired,
};

export default MyButton;
